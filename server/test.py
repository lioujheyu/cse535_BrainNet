#!/usr/bin/env python3

import glob
import argparse
import os
import sys
import pickle

import numpy as np
from sklearn import svm
from sklearn.naive_bayes import GaussianNB
from train import FeatureExtFromEdf

PROBABILITY_THRESHOLD = 0.5

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Predict ')
    parser.add_argument('-d', '--dir', help='The folder containing edf files')
    parser.add_argument('-m', '--model_file', help='The file name of trained svm model. Default to model.dat')
    args = parser.parse_args()
    if args.dir is None:
        print("Directory is not specified ! Use the default path (./test)")
        args.dir = '/home/jliou4/cse535_BrainNet/data/test_data'
        #exit()

    if args.model_file is None:
        args.model_file = 'model.dat'

    # Load edf file
    brainActList = []
    labelList = []
    print('Number of edf files: {}'.format(len(glob.glob(args.dir + '/*.edf'))))
    for fileFullPath in glob.glob(args.dir + '/*.edf'):
        fileName = os.path.basename(fileFullPath)
        # use the file name for the label
        ID = int(fileName[1:4])
        featureSet = FeatureExtFromEdf(fileFullPath)
        brainActList.append(featureSet.tolist())
        labelList.append(ID)
        sys.stdout.write('.')
        sys.stdout.flush()
    print ('')

    # Load trained SVM model
    with open(args.model_file, 'rb') as pickleFile:
        clf = pickle.load(pickleFile)

    probMat = clf.predict_proba(brainActList)
    print (probMat)
    predList = clf.predict(brainActList)

    # for i in range(0, len(predList)):
    #     if np.max(probMat[i]) < PROBABILITY_THRESHOLD:
    #         predList[i] = -1

    print (labelList)
    print (predList)
    with open('./result', 'w') as resultFile:
        resultFile.write('Predict,Origin\n')
        for i in range(0, len(labelList)):
            resultFile.write('{},{}\n'.format( str(predList[i]), str(labelList[i]) ) )
