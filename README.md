# BraiNet for CSE535
The project is to read brain signal from https://physionet.org/pn4/eegmmidb/ as a biometric information for user authentication
The main idea is to use machine learning algorithm to build a model against brain signal and to classify and recognize the new
input signal

This repository is consist of 2 major parts, a android app as a client program and server-side program written in PHP and python.
They are under Project folder and server folder respectively.

## Requirement
* Android SDK for android app compilation
* Apache server with php module to host file uploading and downloading services in server/test_UploadToServer.php and server/train_UploadToServer.php
* python libraries are list as follows
  * numpy and scipy for FFT functionality
  * sciket-learn for machine learning algorithm
  * pyedflib to read
