param(
    [string]$Document = (Join-Path $PSScriptRoot 'vitaemanager-modul-4-npc-dialog-quest.md'),
    [string]$Project = $PSScriptRoot,
    [switch]$CheckOnly
)
$ErrorActionPreference = 'Stop'
$utf8 = New-Object System.Text.UTF8Encoding($false)
$root = [IO.Path]::GetFullPath($Project)
$docPath = [IO.Path]::GetFullPath($Document)
if (-not (Test-Path -LiteralPath $docPath -PathType Leaf)) { throw 'File Markdown tidak ditemukan.' }
$doc = [IO.File]::ReadAllText($docPath, $utf8)
$base = 'src/main/java/com/bangzachery/vitae/vitaemanager/'
$names = @('npc/NpcData.java','npc/NpcStore.java','npc/NpcRenderer.java','npc/NpcQuest.java',
    'npc/NpcService.java','npc/NpcCommand.java','npc/NpcModule.java','integration/VillagerTradeListener.java')
$allowed = @($names | ForEach-Object { $base + $_ })
$sourceBlocks = [regex]::Matches($doc, '(?ms)^### FILE: (?<path>[^\r\n]+)\r?\n\r?\n```java\r?\n(?<code>.*?)\r?\n```[ \t]*\r?$')
if ($sourceBlocks.Count -ne $allowed.Count) { throw 'Jumlah blok sumber tidak lengkap. Unduh Markdown utuh sebelum memasang.' }
$writes = [ordered]@{}
foreach ($block in $sourceBlocks) {
    $path = $block.Groups['path'].Value
    if ($allowed -notcontains $path -or $writes.Contains($path)) { throw 'Path sumber tidak dikenali atau duplikat.' }
    $writes[$path] = $block.Groups['code'].Value + "`n"
}
function Read-ProjectFile([string]$path) {
    $absolute = Join-Path $root $path
    if (-not (Test-Path -LiteralPath $absolute -PathType Leaf)) { throw "File proyek belum ditemukan: $path" }
    return [IO.File]::ReadAllText($absolute, $utf8)
}
function Replace-One([string]$text, [string]$find, [string]$replacement) {
    $hits = [regex]::Matches($text, [regex]::Escape($find))
    if ($hits.Count -ne 1) { throw "Titik integrasi tidak tunggal: $find. File belum diubah." }
    return $text.Substring(0, $hits[0].Index) + $replacement + $text.Substring($hits[0].Index + $hits[0].Length)
}
function Add-YamlEntry([string]$text, [string]$section, [string]$key, [string]$body) {
    $pattern = '(?ms)^' + [regex]::Escape($section) + ':[ \t]*\r?\n(?<body>.*?)(?=^[^ \t#\r\n][^\r\n]*:|\z)'
    $matches = [regex]::Matches($text, $pattern)
    if ($matches.Count -ne 1) { throw "Bagian YAML $section tidak ditemukan atau duplikat. File belum diubah." }
    $m = $matches[0]; $old = $m.Groups['body'].Value
    if ([regex]::IsMatch($old, '(?m)^  ' + [regex]::Escape($key) + ':')) { return $text }
    $updated = $section + ":`n" + $old.TrimEnd() + "`n" + $body.TrimEnd() + "`n`n"
    return $text.Substring(0, $m.Index) + $updated + $text.Substring($m.Index + $m.Length)
}
$mainPath = $base + 'Vitaemanager.java'
$main = Read-ProjectFile $mainPath
$start = 'com.bangzachery.vitae.vitaemanager.npc.NpcModule.start(this, viti);'
$stop = 'com.bangzachery.vitae.vitaemanager.npc.NpcModule.stop(this);'
$legacy = [regex]::Match($main, '(?<field>\w+)\s*=\s*new\s+(?:com\.bangzachery\.vitae\.vitaemanager\.npc\.)?NpcService\s*\(\s*this\s*,\s*viti\s*\)')
if ($legacy.Success) {
    $field = [regex]::Escape($legacy.Groups['field'].Value)
    $patterns = @(
        ($field + '\.start\s*\(\s*\)'),
        ($field + '\.close\s*\(\s*\)'),
        ('registerEvents\s*\(\s*' + $field + '\s*,\s*this\s*\)')
    )
    foreach ($pattern in $patterns) {
        if (-not [regex]::IsMatch($main, $pattern)) { throw 'Integrasi NpcService lama tidak lengkap. File utama belum diubah.' }
    }
    if ($main.Contains($start)) { throw 'Ditemukan dua cara startup NPC. File utama belum diubah.' }
} else {
    if (-not $main.Contains($start)) {
        $anchor = $null
        foreach ($candidate in @('evolution.start();','whispers.start();','vitiListener.start();')) {
            if ($main.Contains($candidate)) { $anchor = $candidate; break }
        }
        if ($null -eq $anchor) { throw 'Titik startup Viti/bisikan/evolusi tidak ditemukan. File utama belum diubah.' }
        $main = Replace-One $main $anchor ($anchor + "`n            " + $start)
    }
    if (-not $main.Contains($stop)) {
        $match = [regex]::Matches($main, 'public\s+void\s+onDisable\s*\(\s*\)\s*\{')
        if ($match.Count -ne 1) { throw 'onDisable tidak tunggal. File utama belum diubah.' }
        $main = $main.Insert($match[0].Index + $match[0].Length, "`n        " + $stop)
    }
}
$writes[$mainPath] = $main
$vitiPath = $base + 'economy/VitiService.java'
$viti = Read-ProjectFile $vitiPath
if (-not [regex]::IsMatch($viti, 'public\s+void\s+reward\s*\(\s*Player\s+player\s*,\s*UUID\s+receipt\s*,\s*BigDecimal\s+amount\s*,\s*Consumer<String>\s+done\s*\)')) {
    if ([regex]::IsMatch($viti, '\bvoid\s+reward\s*\(')) { throw 'Metode reward berbeda dari versi modul ini. VitiService belum diubah.' }
    $reward = @'
    // Stable receipt: retrying an NPC payment cannot credit the balance twice.
    public void reward(Player player, UUID receipt, BigDecimal amount, Consumer<String> done) {
        if (!ready(done)) return;
        if (current.redeemed().contains(receipt)) { done.accept("viti-success"); return; }
        transact(state -> seed(state, player).redeem(player.getUniqueId(), receipt, amount, true), () -> {}, done);
    }

'@
    $viti = Replace-One $viti '    public void reload(Consumer<String> done) {' ($reward + "`n    public void reload(Consumer<String> done) {")
}
$writes[$vitiPath] = $viti
$ymlPath = 'src/main/resources/plugin.yml'
$yml = Read-ProjectFile $ymlPath
$yml = Add-YamlEntry $yml 'commands' 'vnpc' "  vnpc:`n    description: Setup NPC dialog dan quest Vitae`n    usage: /vnpc help`n    aliases: [vitaenpc]"
$yml = Add-YamlEntry $yml 'commands' 'quest' "  quest:`n    description: Status atau batalkan quest Vitae`n    usage: /quest status`n    aliases: [vitaequest]"
$yml = Add-YamlEntry $yml 'permissions' 'vitae.admin.npc' "  vitae.admin.npc:`n    description: Mengatur NPC dialog dan quest`n    default: op"
$yml = Add-YamlEntry $yml 'permissions' 'vitae.npc' "  vitae.npc:`n    description: Memulai dialog NPC Vitae`n    default: true"
$yml = Add-YamlEntry $yml 'permissions' 'vitae.quest' "  vitae.quest:`n    description: Menerima dan membatalkan quest Vitae`n    default: true"
$admin = [regex]::Match($yml, '(?ms)^  vitae\.admin:[ \t]*\r?\n(?<body>.*?)(?=^  [^ \t#\r\n][^\r\n]*:|^[^ \t#\r\n][^\r\n]*:|\z)')
if ($admin.Success -and -not $admin.Value.Contains('      vitae.admin.npc: true')) {
    $updated = $admin.Value
    $children = [regex]::Match($updated, '(?m)^    children:[ \t]*\r?\n')
    if ($children.Success) { $updated = $updated.Insert($children.Index + $children.Length, "      vitae.admin.npc: true`n") }
    else { $updated = $updated.TrimEnd() + "`n    children:`n      vitae.admin.npc: true`n" }
    $yml = $yml.Substring(0,$admin.Index) + $updated + $yml.Substring($admin.Index+$admin.Length)
}
$soft = [regex]::Match($yml, '(?m)^softdepend:[ \t]*\[(?<plugins>[^\]\r\n]*)\][ \t]*\r?$')
if (-not $soft.Success) { throw 'softdepend harus berupa daftar satu baris pada versi pemasang ini. plugin.yml belum diubah.' }
$plugins = @($soft.Groups['plugins'].Value.Split(',') | ForEach-Object { $_.Trim() } | Where-Object { $_ })
foreach ($name in @('ModelEngine','Shopkeepers')) { if ($plugins -notcontains $name) { $plugins += $name } }
$newSoft = 'softdepend: [' + ($plugins -join ', ') + ']'
$yml = $yml.Substring(0,$soft.Index) + $newSoft + $yml.Substring($soft.Index+$soft.Length)
$writes[$ymlPath] = $yml
$gradlePath = 'build.gradle.kts'
$gradle = Read-ProjectFile $gradlePath
if (-not $gradle.Contains('paperweight-mappings-namespace')) {
    $gradle = $gradle.TrimEnd() + "`n`ntasks.jar {`n    manifest.attributes[`"paperweight-mappings-namespace`"] = `"mojang`"`n}`n"
}
$writes[$gradlePath] = $gradle
if ($CheckOnly) {
    Write-Host ('Pemeriksaan berhasil. File yang akan dipasang/diperbarui: ' + $writes.Count)
    foreach ($path in $writes.Keys) { Write-Host $path }
    return
}
$backup = Join-Path $root ('npc-backup-' + [DateTime]::Now.ToString('yyyyMMdd-HHmmss-fff'))
$originals = @{}; $changed = New-Object System.Collections.Generic.List[string]
foreach ($path in $writes.Keys) {
    $absolute = Join-Path $root $path
    if (Test-Path -LiteralPath $absolute -PathType Leaf) {
        $saved = Join-Path $backup $path
        [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($saved)) | Out-Null
        [IO.File]::Copy($absolute, $saved, $false); $originals[$path] = $saved
    }
}
try {
    foreach ($path in $writes.Keys) {
        $absolute = Join-Path $root $path
        [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($absolute)) | Out-Null
        $changed.Add($path); [IO.File]::WriteAllText($absolute, $writes[$path], $utf8)
    }
} catch {
    foreach ($path in $changed) {
        $absolute = Join-Path $root $path
        if ($originals.ContainsKey($path)) { [IO.File]::Copy($originals[$path], $absolute, $true) }
        elseif (Test-Path -LiteralPath $absolute) { Remove-Item -LiteralPath $absolute }
    }
    throw
}
Write-Host 'NPC selesai dipasang. Isi modul lama di Vitaemanager.java dan VitiService.java dipertahankan.'
Write-Host ('Salinan sebelum pemasangan: ' + $backup)
Write-Host 'Berikutnya jalankan .\gradlew.bat clean build'
