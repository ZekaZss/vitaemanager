$ErrorActionPreference = 'Stop'
$folderVitae = '.\src\main\java\com\bangzachery\vitae\vitaemanager'
$listenerVitae = Join-Path $folderVitae 'integration\VillagerTradeListener.java'
if (-not (Test-Path -LiteralPath $listenerVitae -PathType Leaf)) {
    throw 'VillagerTradeListener.java belum ada. Jalankan pemasang NPC terlebih dahulu.'
}
$fileVitae = (Resolve-Path -LiteralPath (Join-Path $folderVitae 'Vitaemanager.java')).Path
$utf8Vitae = New-Object System.Text.UTF8Encoding($false)
$isiVitae = [IO.File]::ReadAllText($fileVitae, $utf8Vitae)
$polaVitae = 'registerEvents\s*\(\s*new\s+(?:com\.bangzachery\.vitae\.vitaemanager\.integration\.)?VillagerTradeListener\s*\(\s*\)\s*,\s*this\s*\)'
if ([regex]::IsMatch($isiVitae, $polaVitae)) {
    Write-Host 'Listener villager trade sudah terdaftar. Tidak ditambahkan lagi.'
} else {
    $titikVitae = [regex]::Matches($isiVitae, 'public\s+void\s+onEnable\s*\(\s*\)\s*\{\s*try\s*\{')
    if ($titikVitae.Count -ne 1) {
        throw 'Susunan onEnable tidak dikenali. File utama belum diubah.'
    }
    $barisVitae = "`r`n            getServer().getPluginManager().registerEvents(new com.bangzachery.vitae.vitaemanager.integration.VillagerTradeListener(), this);"
    $baruVitae = $isiVitae.Insert($titikVitae[0].Index + $titikVitae[0].Length, $barisVitae)
    $salinanVitae = $fileVitae + '.trade-backup-' + [DateTime]::Now.ToString('yyyyMMdd-HHmmss-fff')
    Copy-Item -LiteralPath $fileVitae -Destination $salinanVitae -ErrorAction Stop
    [IO.File]::WriteAllText($fileVitae, $baruVitae, $utf8Vitae)
    Write-Host 'Blokir villager trade ditambahkan. Isi modul lain dipertahankan.'
}
