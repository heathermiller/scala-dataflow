% clear all;
load('data/maglite_2012-06-01T13.24.38_bench.mat');
% 32 cores

% insert, size 5 million
clqinsert = maglite(382:387,:);
ltqinsert = maglite(13:18,:);
slfpinsert = maglite(404:408,:); % only 16 cores
mlfpinsert = maglite(95:100,:);

% map, size 5 million
ltqmap = maglite(668:673,:);
slfpmap = maglite(581:586,:);
mlfpmap = maglite(235:240,:);

% reduce, size 5 million
ltqreduce = maglite(284:289,:);
slfpreduce = maglite(637:642,:);
mlfpreduce = maglite(512:517,:);

% hist, size 1 million
ltqhist = maglite(304:309,:);
slfphist = maglite(560:565,:);
mlfphist = maglite(340:345,:);