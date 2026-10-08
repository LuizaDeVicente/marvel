# Fix Compilation Errors and Project Structure

The project currently fails to build due to missing dependencies (GSON), missing classes (`PublisherRefDto`), and naming inconsistencies between files and class definitions.

## User Review Required

> [!IMPORTANT]
> I will be renaming several DTO classes and the Splash Activity to follow standard Java/Android naming conventions. Please ensure these changes don't conflict with any external expectations you might have.

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///C:/Users/luizavicente-ieg/AndroidStudioProjects/MarvelCerto/gradle/libs.versions.toml)
- Add GSON library version and definition.

#### [MODIFY] [build.gradle.kts (app)](file:///C:/Users/luizavicente-ieg/AndroidStudioProjects/MarvelCerto/app/build.gradle.kts)
- Add GSON implementation dependency.

---

### Data Transfer Objects (DTOs)

#### [MODIFY] [Imagedto.java](file:///C:/Users/luizavicente-ieg/AndroidStudioProjects/MarvelCerto/app/src/main/java/com/example/marvelcerto/dto/Imagedto.java) (Rename to ImageDto.java)
- Rename file to `ImageDto.java`.
- Update class name to `ImageDto`.

#### [MODIFY] [Volumedto.java](file:///C:/Users/luizavicente-ieg/AndroidStudioProjects/MarvelCerto/app/src/main/java/com/example/marvelcerto/dto/Volumedto.java) (Rename to VolumeDto.java)
- Rename file to `VolumeDto.java`.
- Update class name to `VolumeDto`.
- Fix references to `ImageDto`.

#### [MODIFY] [Teamdto.java](file:///C:/Users/luizavicente-ieg/AndroidStudioProjects/MarvelCerto/app/src/main/java/com/example/marvelcerto/dto/Teamdto.java) (Rename to TeamDto.java)
- Rename file to `TeamDto.java`.
- Update class name to `TeamDto`.
- Fix references to `ImageDto`.

#### [NEW] [PublisherRefDto.java](file:///C:/Users/luizavicente-ieg/AndroidStudioProjects/MarvelCerto/app/src/main/java/com/example/marvelcerto/dto/PublisherRefDto.java)
- Create new DTO class for publisher information containing `id` and `name`.

---

### Activities

#### [MODIFY] [activity_splash.java](file:///C:/Users/luizavicente-ieg/AndroidStudioProjects/MarvelCerto/app/src/main/java/com/example/marvelcerto/activity_splash.java) (Rename to SplashActivity.java)
- Rename file to `SplashActivity.java`.
- Update class name to `SplashActivity`.

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/luizavicente-ieg/AndroidStudioProjects/MarvelCerto/app/src/main/AndroidManifest.xml)
- Update activity name from `.activity_splash` to `.SplashActivity`.

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to verify that the project builds successfully.

### Manual Verification
- None required for compilation fixes, but will verify the app launches if possible.
