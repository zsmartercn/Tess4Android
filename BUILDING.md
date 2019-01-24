# Building

This project may be built manually as an alternative to including the pre-built
AAR as an external dependency in your app project. To start the build, import this
project into Android studio. The project can be compiled as a whole or separately
by modules. You can choose which module to modify and compile according to your needs.

## _Building ocr_zs module_

This module needs to rely on the tess-two module, make sure you import dependencies
before compiling, In Android Studio, use

Build -> Rebuild Project

or use the following commands:

    ./gradlew assemble

to build or rebuild the project.

## _Building tess-two module_

The Gradle build uses the gradle-stable plugin and the Android NDK to
build the Tesseract and Leptonica native C/C++ code through a call to
`ndk-build` in `build.gradle`. In Android Studio, use

Build -> Rebuild Project

or use the following commands:

    ./gradlew assemble

to build or rebuild the project.

Note: When building from Android Studio, you may need to set the path to your NDK installation.

Edit your local.properties file to include the path to your NDK directory:

    ndk.dir=/path/to/your/android-ndk


# Importing

After building, the code that is generated may be imported into your app
project in Android Studio as a module using

File -> New -> Import Module -> `tess-two` folder

and then adding the dependency to your app/lib module build.gradle:

        dependencies {
            implementation project(':module_name')
        }

# Removing

If you want to remove your app's dependency on this module, reverse
the import process by removing the module using the Project Structure dialog(File->Project Structure),
manually deleting the module subfolder from your app project folder, and removing the module reference
from your app module build.gradle.