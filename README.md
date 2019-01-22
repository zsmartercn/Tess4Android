# Tess4Android
A fork base on tess-two(https://github.com/rmtheis/tess-two/) and Tesseract OCR Engine(https://github.com/tesseract-ocr/tesseract).

We porting Tesseract 4.0(final) to tess-two project and rewrite dot product function with ARM NEON.

This project works with:

- Tesseract 4.0.0 
- tess-two 9.0.0
- Leptonica 1.74.3
- libjpeg 9b
- libpng 1.6.25

# Pre-requisites
- Android 5.0 or higher
- A v3.05  or higher trained data file for a language. Data files must be copied to 
the Android device in a subdirectory named tessdata.
