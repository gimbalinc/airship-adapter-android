# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed

- Potential crashes upon initialization when Airship has not yet started
- Crashes when calling `AirshipAdapter.isStarted` before `Gimbal.setApiKey` has had a chance to run

### Removed

- `startWithPermissionPrompt()` methods -- the app knows best which permissions to request and when to request them

### Changed

- Use `gimbal-sdk` 4.9.1 and `urbanairship-core` 16.9.0.
- Target / compile against Android API 33
- Adapter now has non-transitive `compileOnly` dependency on `urbanairship-core` -- was transitive `implementation`
- Allow changing of API key when adapter is already started -- no longer require to be preceded by `stop`
- Remove Airship Channel listener when stopping the Adapter
- Parameters for event tracking preferences are primitive `boolean` instead of nullable `Boolean`

## [1.0.0] - 2022-08-22

### Added

- Initial Release
