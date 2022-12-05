//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[ClaimedPaths](index.md)

# ClaimedPaths

[common]\
class [ClaimedPaths](index.md)

[ClaimedPaths](index.md) tracks generated files' Paths. [Schema handlers](../-schema-handler/index.md) who generate files are to [claim](claim.md) each generated file's path. It is an error to generate different [Type](../-type/index.md)s or [Service](../-service/index.md)s to the same file and [ClaimedPaths](index.md) will throw when generation conflicts happen.

## Constructors

| | |
|---|---|
| [ClaimedPaths](-claimed-paths.md) | [common]<br>fun [ClaimedPaths](-claimed-paths.md)() |

## Functions

| Name | Summary |
|---|---|
| [claim](claim.md) | [common]<br>fun [claim](claim.md)(path: Path, service: [Service](../-service/index.md))<br>Tracks that [service](claim.md) has been generated to [path](claim.md).<br>[common]<br>fun [claim](claim.md)(path: Path, type: [Type](../-type/index.md))<br>Tracks that [type](claim.md) has been generated to [path](claim.md). |
