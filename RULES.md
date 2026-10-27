# Minecraft Plugin Development Rules

This document defines the mandatory development rules for this Minecraft plugin project.

Every AI coding agent and developer working on this project MUST follow these rules.

These rules are universal and must not assume what type of plugin is being developed.

Project-specific requirements belong in PROJECT.md and ARCHITECTURE.md.

If a task conflicts with this document, STOP and explain the conflict before modifying the project.

---

# 1. PROJECT PRINCIPLES

The project prioritizes:

- Stability
- Correctness
- Performance
- Maintainability
- Modularity
- Readability
- Data integrity
- Minimal unnecessary dependencies
- Long-term compatibility
- Safe integration with external APIs

Do NOT add systems simply because they may be useful in the future.

Implement only what is currently required.

Avoid unnecessary complexity.

Prefer the simplest architecture that correctly solves the current problem while remaining reasonably extensible.

---

# 2. SOURCE OF TRUTH

Before implementing anything, determine the project's source of truth.

Priority:

1. Current user/task requirements.
2. RULES.md.
3. PROJECT.md.
4. ARCHITECTURE.md.
5. Existing implementation.
6. Other project documentation.

If documentation and implementation disagree, investigate before changing either.

Never silently choose one interpretation when the conflict could affect compatibility, persistent data, architecture, or existing behavior.

---

# 3. BEFORE WRITING CODE

Before modifying the project:

1. Read RULES.md completely.
2. Read PROJECT.md if it exists.
3. Read ARCHITECTURE.md if it exists.
4. Inspect the existing project structure.
5. Inspect the build configuration.
6. Identify the Minecraft server platform and target version.
7. Identify the Java version.
8. Identify relevant dependencies and their versions.
9. Search for existing implementations related to the task.
10. Understand dependencies between affected systems.
11. Determine the smallest safe implementation.

Never immediately create new classes without inspecting the existing project.

Do NOT assume a system is missing simply because its name differs from what you expected.

Do NOT assume the project follows common conventions without verifying them.

---

# 4. TASK SCOPE

Only implement what the current task requests.

Do NOT:

- Add unrelated features.
- Refactor unrelated systems.
- Rename unrelated classes.
- Move unrelated files.
- Change unrelated APIs.
- Reformat the entire project.
- Introduce speculative abstractions.
- Add future systems without a current requirement.
- Rewrite working code merely because another style is preferred.
- Upgrade dependencies unless required.
- Change configuration formats unnecessarily.

If another change is necessary to complete the task safely, explain why.

Prefer small, controlled, reviewable changes.

---

# 5. PROJECT INSPECTION

Before creating a new:

- Class
- Interface
- Service
- Manager
- Utility
- Listener
- Command
- Repository
- Adapter
- Configuration system
- Database abstraction
- API

search the project for existing functionality that may already provide the same responsibility.

Reuse existing systems when appropriate.

Do not create duplicate implementations simply because creating a new class is easier.

---

# 6. ARCHITECTURE

Architecture must follow the actual needs of the project.

Prefer domain-based or responsibility-based organization.

Example:

src/main/java/<base-package>/

core/
feature/
command/
listener/
data/
integration/
api/

This is an example, NOT a mandatory package structure.

Do not force every project into the same architecture.

Avoid giant generic packages such as:

manager/
utils/
handlers/
misc/

when more specific ownership can be expressed.

Architecture should make it obvious where functionality belongs.

---

# 7. SINGLE RESPONSIBILITY

Classes should have one clear primary responsibility.

Avoid god classes that:

- Handle commands
- Store state
- Access databases
- Parse configuration
- Execute business logic
- Integrate external APIs
- Register listeners

all at once.

Split responsibilities when doing so improves clarity and maintainability.

Do not split trivial behavior into dozens of meaningless classes merely to satisfy a pattern.

Balance matters.

---

# 8. OWNERSHIP

Every important piece of mutable state or business logic should have a clear owner.

Other systems should interact with that owner through intentional methods or APIs instead of modifying internal state directly.

Avoid situations where multiple unrelated classes can independently mutate the same important state.

Ownership should be obvious from the architecture.

---

# 9. DEPENDENCY INJECTION

Prefer explicit dependencies.

Constructor injection is preferred when practical.

Example:

public ExampleService(
    DataRepository repository,
    ExternalProvider provider
) {
    this.repository = repository;
    this.provider = provider;
}

Avoid turning the main plugin class into a global service locator.

Avoid unnecessary static global access.

Static state should have a clear justification.

Dependencies should be visible and understandable.

---

# 10. EXTERNAL PLUGIN INTEGRATIONS

Do not spread third-party API calls throughout unrelated systems.

When an external plugin provides significant functionality, isolate its integration behind an adapter, provider, gateway, bridge, or similarly clear boundary when practical.

Example:

Internal System
    ↓
Provider Interface
    ↓
ExternalPluginAdapter
    ↓
External Plugin API

This reduces coupling and makes API changes easier to handle.

Small integrations do not require pointless abstraction layers.

Use judgment.

---

# 11. EXTERNAL API VERIFICATION

Never invent external APIs.

Before using:

- Paper API
- Bukkit API
- Spigot API
- Folia API
- Third-party plugin APIs
- External Java libraries

verify:

1. Dependency name.
2. Dependency version.
3. Class existence.
4. Method existence.
5. Method signature.
6. Required thread/context.
7. Relevant lifecycle requirements.

Prefer official documentation, Javadocs, source code, or dependencies already present in the project.

Do not rely on memory when API details can be verified.

---

# 12. OPTIONAL DEPENDENCIES

Optional integrations must fail safely.

If an optional dependency is unavailable:

- Do not crash the entire plugin unnecessarily.
- Disable only affected functionality when possible.
- Log a clear explanation.
- Keep unrelated functionality operational.

Never assume optional dependencies exist.

Detect them properly during plugin startup or integration initialization.

---

# 13. DEPENDENCIES

Do not add dependencies unnecessarily.

Before adding one:

1. Explain what problem it solves.
2. Check whether Java already provides the functionality.
3. Check whether the server platform already provides it.
4. Consider maintenance.
5. Consider compatibility.
6. Consider runtime size.
7. Consider performance.
8. Consider whether shading/relocation is required.

Avoid adding large libraries for trivial functionality.

Pin or control dependency versions appropriately.

---

# 14. PAPER AND SERVER API

Prefer the supported API of the project's target server platform.

For Paper projects:

Prefer Paper API when it provides the required functionality.

Do not use CraftBukkit or NMS simply because it appears more powerful.

Use internal Minecraft server code only when the supported API cannot reasonably accomplish the task.

If version-specific implementation is necessary:

- Isolate it.
- Document it.
- Minimize its surface area.
- Do not spread version-specific code throughout the project.

---

# 15. VERSION COMPATIBILITY

The project's supported:

- Minecraft version
- Server platform
- Java version
- Dependency versions

must be treated as explicit constraints.

Do not silently change the target version.

Do not claim compatibility with versions that have not been verified.

Avoid version checks scattered throughout the codebase.

If multi-version support is required, design a clear compatibility boundary.

---

# 16. CONFIGURATION

Configuration must be predictable and validated.

Configuration systems should:

- Provide sensible defaults where appropriate.
- Validate required values.
- Validate types.
- Reject invalid critical values.
- Produce understandable errors.
- Avoid silently hiding configuration mistakes.

Useful configuration errors should include when possible:

- File
- Path/key
- Invalid value
- Reason
- Expected format/value

Do not silently rewrite user configuration unless explicitly designed to do so.

---

# 17. NO UNNECESSARY HARDCODING

Values likely to change between deployments should not be buried unnecessarily in Java code.

Examples may include:

- Messages
- Limits
- Durations
- Feature settings
- Gameplay values
- External identifiers

However, do not move every constant into configuration.

Technical constants and implementation details may remain in code.

Configuration exists for values users or administrators reasonably need to change.

---

# 18. IDENTIFIERS

Persistent internal identifiers must be stable.

Do not use mutable display text as a persistent identifier.

Prefer:

stable_id

over:

"Pretty Display Name"

Changing presentation should not corrupt persistent references.

IDs should follow a consistent naming convention.

---

# 19. DATA INTEGRITY

Persistent data must be treated carefully.

Never casually:

- Delete existing data.
- Overwrite data with defaults.
- Reset unknown fields.
- Change identifier meaning.
- Change serialization format.
- Recreate database tables destructively.

When changing persistent formats, consider migration and backward compatibility.

Data integrity has higher priority than implementation convenience.

---

# 20. PLAYER IDENTIFICATION

When persistent player identity is required, use UUIDs.

Do not use Minecraft usernames as permanent database identifiers.

Names may change.

Use names for display/search convenience only when appropriate.

---

# 21. DATABASE

Database access must have clear ownership.

Avoid database logic scattered throughout commands, listeners, and unrelated systems.

Potentially slow database operations must not unnecessarily block the server thread.

Use:

- Prepared statements
- Proper resource management
- Transactions when atomicity is required
- Controlled connection lifecycle

Never construct SQL by concatenating untrusted values.

---

# 22. DATA MIGRATION

Persistent schemas should support controlled evolution when necessary.

When changing:

- Database schema
- Serialized data
- Configuration format
- Persistent identifiers

consider migrations.

Migration operations should:

- Preserve existing data.
- Be deterministic.
- Be version-aware where appropriate.
- Fail safely.
- Log meaningful information.

Do not solve schema changes by deleting production data.

---

# 23. THREADING

Never assume asynchronous execution is automatically better.

Potentially blocking work may include:

- Database operations
- Large file operations
- Network operations
- Heavy computation

These may belong off the main server thread.

However, many Minecraft server APIs are not thread-safe.

World/entity/player operations must execute in the context required by the target server platform.

Never call thread-unsafe APIs asynchronously.

Correctness comes before asynchronous cleverness.

---

# 24. FOLIA COMPATIBILITY

Do not claim Folia compatibility unless the project is intentionally designed and tested for it.

If Folia support is required:

- Use the appropriate schedulers.
- Respect region ownership.
- Avoid assumptions based on Bukkit's traditional single main thread.
- Verify third-party dependencies are compatible.

Paper compatibility does not automatically mean Folia compatibility.

---

# 25. PERFORMANCE

Minecraft servers are latency-sensitive.

Avoid:

- Unnecessary per-tick tasks.
- Scanning all online players every tick.
- Scanning large worlds repeatedly.
- Repeated parsing of unchanged configuration.
- Repeated database queries for unchanged data.
- Heavy work inside frequently fired events.
- Excessive allocations in hot paths.
- Blocking I/O on server threads.

Prefer:

- Events
- Caching
- Targeted updates
- Batching
- Lazy computation
- Appropriate scheduling

Do not optimize blindly.

Identify actual or likely hot paths first.

---

# 26. CACHING

Cache only when it provides meaningful value.

Every cache must have a clear:

- Owner
- Lifetime
- Invalidation strategy
- Memory consideration

Never introduce caching without knowing how stale values are refreshed or removed.

Avoid memory leaks from player/entity references.

Clean up player-scoped caches when appropriate.

---

# 27. EVENT HANDLING

Listeners should remain focused.

A listener should generally:

1. Validate the event.
2. Extract relevant information.
3. Delegate business logic.

Avoid hundreds of lines of business logic directly inside event handlers.

Pay attention to:

- Event priority
- Event cancellation
- ignoreCancelled
- Event frequency

Do not cancel events unnecessarily.

---

# 28. COMMANDS

Command handlers should primarily handle:

- Argument parsing
- Permission checks
- Validation
- Calling application logic
- User feedback

Do not bury major business logic inside command executors.

Commands should produce useful feedback for invalid usage.

Use appropriate command APIs for the project's target platform.

---

# 29. PERMISSIONS

Administrative or restricted functionality should use permission nodes where appropriate.

Do not rely exclusively on operator status unless explicitly required.

Permission names should follow a consistent namespace.

Example:

plugin.command.feature
plugin.admin.reload

Do not silently grant sensitive functionality because a permission check was inconvenient.

---

# 30. RELOAD

Reload functionality must be designed intentionally.

Never rely on full server reload mechanisms as a substitute for proper lifecycle handling.

Reload only resources the plugin can safely reload.

A reload must not create:

- Duplicate tasks
- Duplicate listeners
- Duplicate registrations
- Stale caches
- Stale configuration references
- Resource leaks

If something cannot safely reload, document that limitation.

---

# 31. RESOURCE LIFECYCLE

Every resource that is created should have a defined lifecycle.

Examples:

- Scheduler tasks
- Database connections
- Executors
- File watchers
- Network clients
- Temporary caches
- Registered services

On plugin disable:

- Cancel owned tasks.
- Close resources.
- Flush critical pending data when safe.
- Unregister resources where necessary.

Do not leave background resources alive after shutdown.

---

# 32. MEMORY SAFETY

Avoid retaining unnecessary references to:

- Player objects
- Entity objects
- World objects
- Large collections
- ClassLoader-related objects

Prefer UUIDs or stable identifiers for long-lived references when appropriate.

Remove temporary/player-specific state when no longer needed.

Do not create memory leaks through static collections.

---

# 33. ERROR HANDLING

Never silently swallow exceptions.

Bad:

try {
    operation();
} catch (Exception ignored) {
}

Handle failures intentionally.

When an operation fails:

- Protect data integrity.
- Log useful context.
- Keep unrelated systems operational when possible.
- Avoid leaving partially mutated state.

Do not catch Exception everywhere simply to suppress crashes.

Catch exceptions where recovery or meaningful context is possible.

---

# 34. LOGGING

Logs should help diagnose problems.

Bad:

Something went wrong.

Better:

Failed to load configuration "rewards.yml":
Path "daily.limit" expected an integer but received "abc".

Avoid console spam.

Debug logging should be configurable when significant debug output exists.

Do not repeatedly print identical recoverable errors every tick.

Never expose:

- Passwords
- Tokens
- Private keys
- Database credentials
- Sensitive configuration values

in logs.

---

# 35. NULL SAFETY

Do not blindly assume values exist.

Validate nullable API results.

Use defensive checks where absence is legitimate.

Do not use Optional everywhere simply because it exists.

Use it when it makes an API contract clearer.

Avoid using null as an undocumented magic state.

---

# 36. INPUT VALIDATION

Treat external input as untrusted.

Examples:

- Commands
- Configuration
- Database values
- Plugin messages
- Network responses
- Third-party API values
- User-entered strings

Validate:

- Type
- Range
- Length
- Format
- Existence
- Permission

before performing sensitive operations.

---

# 37. SECURITY

Never:

- Execute arbitrary user-controlled commands without explicit design.
- Concatenate untrusted SQL.
- Expose credentials in logs.
- Trust client-side claims for authoritative gameplay state.
- Deserialize unsafe arbitrary objects.
- Download/execute remote code without explicit authorization.

Sensitive operations require appropriate validation and permissions.

---

# 38. CODE QUALITY

Readable code is preferred over clever code.

Prefer:

- Descriptive names
- Small focused methods
- Explicit control flow
- Consistent formatting
- Clear ownership
- Limited nesting

Avoid:

- Meaningless abbreviations
- Giant methods
- Deep nesting
- Hidden side effects
- Clever one-liners that harm readability
- Premature abstraction
- Pattern usage merely for appearance

Code should communicate intent.

---

# 39. NAMING

Names should describe responsibility.

Bad:

Manager
Helper
Utils
Thing
Data

when used without meaningful context.

Better:

ConfigurationLoader
CooldownRegistry
PlayerRepository
MessageFormatter
ExternalApiAdapter

Do not create an endless collection of classes ending in Manager simply because naming things is annoying.

---

# 40. COMMENTS

Comments should explain WHY when the reason is not obvious.

Do not narrate obvious code.

Bad:

// Increment count
count++;

Useful:

// The value is incremented before persistence because downstream
// listeners expect the updated state.
count++;

Remove outdated comments when behavior changes.

Incorrect documentation is worse than missing documentation.

---

# 41. API DESIGN

Public APIs must be intentional.

Avoid exposing implementation details.

Prefer stable abstractions where external consumers need them.

Public APIs should:

- Have clear contracts.
- Validate invalid arguments.
- Document important threading requirements.
- Avoid unnecessary breaking changes.

Do not make everything public simply because another class needs access.

Use appropriate visibility.

---

# 42. BACKWARD COMPATIBILITY

Before changing:

- Public APIs
- Configuration
- Database schemas
- Persistent IDs
- Serialized formats
- Commands
- Permission nodes

consider existing users/data.

Breaking changes require explicit justification.

When practical, provide:

- Migration
- Deprecation
- Compatibility handling
- Upgrade instructions

Do not casually break working installations.

---

# 43. TESTABILITY

Separate pure business logic from platform-specific behavior when practical.

Logic that does not require Minecraft should not unnecessarily depend on Minecraft classes.

Important components such as:

- Parsers
- Validators
- State transitions
- Registries
- Serialization
- Calculations

should be testable independently where useful.

Do not distort simple code solely to make everything mockable.

---

# 44. TESTING

When relevant tests exist:

- Run them after changes.
- Add tests for important new pure logic when practical.
- Update tests when intentional behavior changes.

Test:

- Normal behavior
- Important edge cases
- Invalid input
- Failure paths

Do not delete failing tests merely to make the build green.

Determine whether the implementation or the test is wrong.

---

# 45. BUILD VERIFICATION

After implementation:

1. Compile the project.
2. Run relevant tests.
3. Check build output.
4. Investigate new warnings relevant to the change.
5. Validate modified resources/configuration.
6. Verify packaging when relevant.

Do not claim success without actually performing available verification.

If the environment prevents verification, state exactly what could not be verified.

---

# 46. NO FAKE IMPLEMENTATIONS

Never disguise unfinished functionality as completed work.

Do not:

- Return fake success values.
- Hardcode temporary results and call them finished.
- Create empty implementations that silently do nothing.
- Suppress unsupported behavior.
- Claim integration works without implementing it.
- Claim tests passed without running them.

If functionality is intentionally incomplete, make that clear.

---

# 47. REFACTORING

Refactor only when:

- Required for the requested feature.
- Fixing a concrete structural problem.
- Explicitly requested.
- Necessary to safely fix a bug.

Do not combine large unrelated refactors with feature work.

Keep behavior unchanged during pure refactors unless explicitly stated otherwise.

Verify the project after refactoring.

---

# 48. ARCHITECTURE CHANGES

Do not silently change architecture.

If an architectural change is necessary:

1. Identify the current limitation.
2. Explain why the change is necessary.
3. Choose the smallest sufficient change.
4. Identify affected systems.
5. Consider compatibility.
6. Update architectural documentation.

Avoid architecture rewrites when a localized change is sufficient.

---

# 49. DOCUMENTATION

Documentation must describe reality.

Update relevant documentation when changing:

- Public behavior
- Architecture
- Configuration
- Commands
- Permissions
- Dependencies
- Setup requirements
- Persistent formats

Do not document planned functionality as if it already exists.

Do not leave documentation knowingly inconsistent with implementation.

---

# 50. GIT AND CHANGE SAFETY

Keep changes easy to review and revert.

Avoid modifying unrelated files.

Do not:

- Delete large amounts of code without understanding them.
- Rewrite complete files for tiny changes without reason.
- Change line endings across the project.
- Reformat the entire codebase during unrelated work.

When Git is available, inspect the diff after implementation.

Ensure the diff contains only intended changes.

Never discard existing user changes merely because they conflict with your preferred implementation.

---

# 51. EXISTING CODE

Existing code must be treated as intentional until understood.

Before deleting or replacing code:

1. Search its usages.
2. Understand its purpose.
3. Check whether external consumers may depend on it.
4. Check configuration/data compatibility.
5. Verify replacement behavior.

"Looks unused" is not enough evidence to delete something.

---

# 52. GENERATED CODE

Do not regenerate entire files or systems unnecessarily.

Preserve intentional user-written logic.

When editing generated files is inappropriate, modify the generator/source instead.

Do not mix generated and hand-maintained responsibilities without documenting the boundary.

---

# 53. AI ANTI-HALLUCINATION RULES

AI agents MUST NOT:

- Invent classes.
- Invent methods.
- Invent API signatures.
- Invent dependency versions.
- Invent configuration options.
- Invent build results.
- Invent test results.
- Invent file contents that were not inspected.
- Assume a class is unused without searching.
- Assume a plugin/library supports functionality without verification.
- Claim compatibility that was not verified.

When uncertain:

VERIFY.

If verification is impossible and the uncertainty affects correctness, explicitly identify the uncertainty instead of pretending.

---

# 54. AI PROJECT AWARENESS

Before editing, AI agents must inspect enough of the project to understand the relevant context.

Do not blindly modify a single file when behavior depends on:

- Interfaces
- Implementations
- Registrations
- Configuration
- Dependency injection
- Build files
- Resources
- Tests

However, do not read thousands of unrelated files without reason.

Inspect relevant context, not the entire universe.

---

# 55. AI CHANGE PROCEDURE

For every meaningful task, follow:

ANALYZE
↓
PLAN
↓
IMPLEMENT
↓
COMPILE
↓
TEST
↓
REVIEW

ANALYZE:
- Understand the request.
- Inspect relevant existing code.
- Identify constraints.

PLAN:
- Determine the smallest safe change.
- Identify affected files.
- Consider compatibility.

IMPLEMENT:
- Make focused changes.
- Follow existing project conventions.

COMPILE:
- Build the project.

TEST:
- Run relevant available tests or checks.

REVIEW:
- Inspect changes/diff.
- Verify task requirements.

Do not skip directly from request to uncontrolled code generation.

---

# 56. AI RESPONSE AFTER IMPLEMENTATION

After completing a development task, report concisely:

- What changed.
- Files created.
- Files modified.
- Important technical decisions.
- Build/test results.
- Anything that could not be verified.

Do not produce a dramatic essay after changing three lines.

---

# 57. DO NOT GUESS CRITICAL INFORMATION

Do not guess when missing information affects:

- API compatibility
- Data integrity
- Thread safety
- Persistent formats
- Dependency compatibility
- Architecture
- Security
- Server stability

First attempt to determine the information from:

1. Existing project files.
2. Build configuration.
3. Documentation.
4. Installed dependencies.
5. Official external documentation when available.

Only request clarification when the information cannot be safely determined.

---

# 58. DEFINITION OF DONE

A task is DONE only when:

- Requested functionality is implemented.
- Code compiles when build tooling is available.
- Relevant tests pass when available.
- No known critical error remains.
- Existing functionality has not knowingly been broken.
- Persistent data remains safe.
- Documentation is updated when necessary.
- The final diff contains only intended changes.

Generated code alone does NOT mean the task is complete.

---

# 59. PRIORITY ORDER

When requirements conflict, prioritize:

1. Data integrity
2. Server stability
3. Correctness
4. Security
5. Compatibility
6. Maintainability
7. Performance
8. Developer convenience

Performance optimizations must never knowingly compromise correctness or data integrity without an explicit requirement.

---

# 60. FINAL RULE

Do not guess.
Do not fabricate.
Do not hide failures.
Do not silently break existing behavior.
Do not solve problems that were never requested.

Understand the project first.

Make the smallest correct change.

Verify the result.

Leave the project in a better or equally stable state than before.