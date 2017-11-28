#!/usr/bin/python3

import glob
import argparse
import pyedflib
import numpy as np
import scipy.fftpack
import os
from svmutil import *

SAMPLE_STRIDE = 100

def FeatureExtFromEdf(filename, start, stop):
    edfFile = pyedflib.EdfReader(filename)
    n_sensor = edfFile.signals_in_file
    n_sample = edfFile.getNSamples()[0]
    featureSet = np.empty(shape=0)
    for sensorIdx in np.arange(n_sensor):
        channel = edfFile.readSignal(sensorIdx)
        Phase1 = scipy.fftpack.fft(channel)
        Phase2 = abs(Phase1/n_sample)*2
        ChannelFFT = Phase2[0:int(n_sample/2)]
        featureSet = np.concatenate((featureSet, ChannelFFT[start:stop]))

    return featureSet

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Read all edf files under a folder as a personal brain trace')
    parser.add_argument('-d', '--dir', help='The folder containing target edf file')
    args = parser.parse_args()
    if args.dir is None:
        print("Directory not specified !!")
        args.dir = '/home/jliou4/cse535_BrainNet/data/train_data'
        #exit()

    idList = args.dir
    brainActList = []
    labelList = []
    labelDict = {}
    for fileFullPath in glob.glob(args.dir + '/*.edf'):
        fileName = os.path.basename(fileFullPath)
        # use the file name for the label
        ID = int(fileName[1:4])

        if ID in labelDict:
            labelDict[ID] += 1
        else:
            labelDict[ID] = 1

        featureSet = FeatureExtFromEdf(fileFullPath, start=0, stop=10)
        brainActList.append(featureSet.tolist())
        labelList.append(ID)

    print('Number of Sample: {}'.format(len(labelList)))
    print('Start SVM training ... ')
    prob = svm_problem(labelList, brainActList, isKernel=True)
    param = svm_parameter('-t 0 -c 10 -b 1 -v 5')
    model = svm_train(prob, param)


