
# Plot itemset precision-recall
import matplotlib.pyplot as plt
from matplotlib import rc
import numpy as np
import os

rc('xtick', labelsize=16) 
rc('ytick', labelsize=16) 

def main():
    
    #probname = 'caviar'
    probname = 'Background'
    
    cols = ['b','g','c','m','r']
    prefixes = ['IIM','MTV','SLIM','KRIMP','CHARM']
    
    for prefix in prefixes:
    
        precision, recall = readdata(open('/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/PrecisionRecall/'+probname+'/'+prefix+'_'+probname+'_pr.txt'))
	col = cols[prefixes.index(prefix)]
        
	# z-order
	zo = 5
	if prefix == 'MTV':
	    zo = 10

        # Calculate interpolated precision
        pt_recall = np.arange(0,1.1,0.1)
        interp_precision = [pinterp(zip(precision,recall),r) for r in pt_recall]
        plotfigpr(interp_precision,pt_recall,probname,col,1,' (rare)' if 'caviar' in probname else '',zo)
        
    plt.figure(1)   
    plt.legend(prefixes,'lower right')
    plt.show()

# Interpolate precision
def pinterp(prarray,recall):

    m = [p for (p,r) in prarray if r >= recall]
    if(len(m)==0):
        return np.nan
    else:
        return max(m) 

def plotfigpr(precision,recall,probname,col,figno,xlabel_suffix,zo):

    # sort
    ind = np.array(recall).argsort()
    r_d = np.array(recall)[ind]
    p_d = np.array(precision)[ind]

    plt.figure(figno)
    plt.hold(True)
    plt.plot(r_d,p_d,'.-',color=col,linewidth=2,markersize=14,clip_on=False,zorder=zo)
    #plt.title(probname+' top-k precison-recall')
    plt.xlabel('Recall'+xlabel_suffix,fontsize=16)
    plt.ylabel('Precision',fontsize=16)
    plt.xlim([0,1])
    plt.ylim([0,1])
    plt.grid(True)

def plotfigpandr(x,precision,recall,probname,name,col,figno):
    
    plt.figure(figno)
    plt.hold(True)
    plt.plot(x,precision,'.-',linewidth=2,markersize=14,color=col)
    plt.plot(x,recall,'.--',linewidth=2,markersize=14,color=col)
    plt.title(probname+' Precison-Recall - '+name)
    plt.xlabel(name)
    plt.ylabel('Precision/Recall')
    #plt.legend(['Precision','Recall'],'lower left')
    plt.xlim([1,max(x)])
    plt.ylim([-0.1,1.1])
    plt.grid(True)

def readdata(fl):
   
    for line in fl:
      if 'Precision' in line:
	pre = line.strip().split(': ')[1].replace('[','').replace(']','').split(', ')
      if 'Recall' in line:
	rec = line.strip().split(': ')[1].replace('[','').replace(']','').split(', ')

    return (map(float,pre),map(float,rec))

main()
