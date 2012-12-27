clear all;
load('data/lampmac14_2012-06-01T13.38.38_bench.mat')
% 8 cores

% insert, size 5 million
clqinsert = lampmac(197:200,:);
ltqinsert = lampmac(5:8,:);
slfpinsert = lampmac(209:212,:);
mlfpinsert = lampmac(33:36,:);

% map, size 5 million
ltqmap = lampmac(341:344,:);
slfpmap = lampmac(293:296,:);
mlfpmap = lampmac(105:108,:);

% reduce, size 5 million
ltqreduce = lampmac(137:140,:);
slfpreduce = lampmac(325:328,:);
mlfpreduce = lampmac(237:240,:);

% hist, size 1.5 million
ltqhist = lampmac(145:148,:);
slfphist = lampmac(277:280,:);
mlfphist = lampmac(153:156,:);