#!/usr/bin/env python3

import glob
import argparse
import os
import sys
import pickle

import pyedflib
import numpy as np
from scipy.fftpack import fft
from sklearn.model_selection import train_test_split
from sklearn import svm
from sklearn.naive_bayes import GaussianNB

FFT_SAMPLE_START = 0
FFT_SAMPLE_STOP = 20
DATA_PATH = '/home/jliou4/cse535_BrainNet/data'

# This follows the standard fft procedure found on Matlab fft example
def FeatureExtFromEdf(filename):
    edfFile = pyedflib.EdfReader(filename)
    n_sensor = edfFile.signals_in_file
    n_sample = edfFile.getNSamples()[0]
    featureSet = np.empty(shape=0)
    # Read out the sensor's data one by one
    for sensorIdx in np.arange(n_sensor):
        channel = edfFile.readSignal(sensorIdx)
        Phase1 = fft(channel)
        Phase2 = abs(Phase1/n_sample)*2
        ChannelFFT = Phase2[0:int(n_sample/2)]
        featureSet = np.concatenate((featureSet, ChannelFFT[FFT_SAMPLE_START:FFT_SAMPLE_STOP]))

    return featureSet

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Read all edf files and use svm to classify')
    parser.add_argument('-d', '--dir', help='The folder containing edf files')
    parser.add_argument('-m', '--model', help='The type of the training model. Can be svm or gnb')
    args = parser.parse_args()
    if args.dir is None:
        print("Directory is not specified ! Use the default path (./train)")
        args.dir = '/home/jliou4/cse535_BrainNet/data/train_data'
        #exit()
    if args.model is None:
        args.model = 'svm'

    brainActList = []
    # This is for data label
    labelList = []
    # This is only to check how many label are used this time of training
    labelDict = {}
    print('Number of edf files: {}'.format(len(glob.glob(args.dir + '/*.edf'))))
    for fileFullPath in glob.glob(args.dir + '/*.edf'):
        fileName = os.path.basename(fileFullPath)
        # use the file name for the label
        ID = int(fileName[1:4])

        if ID in labelDict:
            labelDict[ID] += 1
        else:
            labelDict[ID] = 1

        featureSet = FeatureExtFromEdf(fileFullPath)
        brainActList.append(featureSet.tolist())
        labelList.append(ID)
        sys.stdout.write('.')
        sys.stdout.flush()
    print ('')

    for i in range(50, 100):
        IDstr = 'S' + str(i).zfill(3)
        fileFullPath = DATA_PATH + '/' + IDstr + '/' + IDstr + 'R13.edf'
        featureSet = FeatureExtFromEdf(fileFullPath)
        brainActList.append(featureSet.tolist())
        labelList.append(-1)
        sys.stdout.write('.')
        sys.stdout.flush()
    print ('')

    # First SVM training for cross validation
    dataTrain, dataTest, labelTrain, labelTest = train_test_split(
        brainActList, labelList, test_size=0.1)
    if args.model == 'svm':
        print('Start SVM training ... ')
        clf = svm.SVC(kernel='linear', decision_function_shape='ovo', probability=False)
    elif args.model == 'gnb':
        print('Start Gaussian Naive Bayes training ... ')
        clf = GaussianNB()
    clf.fit(dataTrain, labelTrain)
    score = clf.score(dataTest, labelTest)
    print ('Cross validation score: {}'.format(score))

    # Second SVM training using all dataset and save the model in a file
    if args.model == 'svm':
        clf = svm.SVC(kernel='linear', decision_function_shape='ovo', probability=True)
    elif args.model == 'gnb':
        clf = GaussianNB()
    clf.fit(brainActList, labelList)
    with open('./model.dat', 'wb') as pickleFile:
        pickle.dump(clf, pickleFile)
