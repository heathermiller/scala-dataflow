clear all;
load('data/wolf_2012-06-01T07.31.17_bench.mat')
% 32 cores

% insert, size 5 million
clqinsert = wolf(93:96,:); % only 8 cores
ltqinsert = wolf(5:8,:); % only 8 cores
slfpinsert = wolf(105:108,:); % only 8 cores
% mlfpinsert = wolf(20:25,:); % full 32 cores
mlfpinsert = wolf(20:23,:); % only 8 cores

% map, size 5 million
ltqmap = wolf(183:186,:); % only 8 cores
slfpmap = wolf(149:152,:); % only 8 cores
% mlfpmap = wolf(41:46,:); % full 32 cores
mlfpmap = wolf(41:44,:); % only 8 cores

% reduce, size 5 million
ltqreduce = wolf(59:62,:); % only 8 cores
slfpreduce = wolf(168:171,:); % only 8 cores
% mlfpreduce = wolf(120:125,:); % full 32 cores
mlfpreduce = wolf(120:123,:); % only 8 cores

% hist, size 1 million
% ltqhist = wolf(72:76,:); % 16 cores
% slfphist = wolf(139:143,:); % 16 cores
% mlfphist = wolf(83:87,:); % 16 cores
ltqhist = wolf(72:75,:); % 8 cores
slfphist = wolf(139:142,:); % 8 cores
mlfphist = wolf(83:86,:); % 8 cores