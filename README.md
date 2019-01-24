# Tess4Android
A fork base on tess-two(https://github.com/rmtheis/tess-two/) and Tesseract OCR Engine(https://github.com/tesseract-ocr/tesseract).

We porting Tesseract 4.0(final) to tess-two project and rewrite dot product function with ARM NEON.We add a full feature demo project.

This project works with:

- Tesseract 4.0.0
- tess-two 9.0.0
- Leptonica 1.74.3
- libjpeg 9b
- libpng 1.6.25



## Pre-requisites
- Android 5.0 or higher
- A v3.05  or higher trained data file for a language. Data files must be copied to
the Android device in a subdirectory named tessdata.

## Usage

To use tess-two from your app, edit your app module's `build.gradle` file to add
tess-two as an external dependency:

	dependencies {
	    implementation 'com.zsmarter:Tess4Android:1.0.0'
	}
	
## Building
If you want to modify the Tess4Android code, you can build the project locally. 

## Versions
Release points are tagged with [version numbers][semantic-versioning]. A change 
to the major version number indicates an API change making that version incompatible 
with previous versions.

The [change log](CHANGELOG.md) shows what's new in each version.

## License

    (C) Copyright 2018, ZSmarter Technology Co, Ltd.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
