Proto Pruner
============

An experimental tool for pruning a proto tree into a smaller subset for compilation
by other tools.

```
Arguments: --in=dir --out=dir [--includes=pattern] [--excludes=pattern] [file]

Patterns must be in one of the following formats:
  * Partial package name followed by a `*` (e.g., `com.example.*`).
  * Full package name of a type or service.
  * Full package name, a `#`, and the name of a member.

Multiple patterns can be supplied to a single argument by separating them by
a `,` (e.g., `--include=com.example.*,com.other.package.*`).

File must contain patterns prefixed with either `+` or `-` to signify an include
or exclude, respectively. All other lines are ignored.
```
