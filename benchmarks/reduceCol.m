close all;

cols = [ 1 0 0 ; 0 .7 0 ; 0 0 1 ; .6 0 .8 ];
par = [1 2 4 8];



figure('units','pixels','Position',[1 1 1440 600]);

% WOLF
load('data/wolf_insert_map_reduce_hist.mat');
subplot(133)
set(gca,'YScale','log');
set(gca,'Xtick',1:4);
set(gca,'XTickLabel',par);
set(gca,'FontSize',15);
grid on;
hold on;

title('32-core Xeon');
xlabel('Number of CPUs');

plotScaling(1:length(par),ltqreduce,'--sq',cols(1,:)); 
plotScaling(1:length(par),slfpreduce,'-x',cols(3,:));
plotScaling(1:length(par),mlfpreduce,'-d',cols(4,:)); 
l1 = legend('Java LTQ','SingleLane FlowPool','MultiLane FlowPool','Location','SouthEast');
set(l1,'FontSize',12,'Color',[1 1 1]);

axis([1 4 10 10000]);
hold off;

% LAMPMAC
load('data/lampmac_insert_map_reduce_hist.mat');
subplot(132)
set(gca,'YScale','log');
set(gca,'Xtick',1:4);
set(gca,'XTickLabel',par);
set(gca,'FontSize',15);
grid on;
hold on;

title('4-core i7');
xlabel('Number of CPUs');

plotScaling(1:length(par),ltqreduce,'--sq',cols(1,:)); 
plotScaling(1:length(par),slfpreduce,'-x',cols(3,:));
plotScaling(1:length(par),mlfpreduce,'-d',cols(4,:));
l1 = legend('Java LTQ','SingleLane FlowPool','MultiLane FlowPool','Location','NorthEast');
set(l1,'FontSize',12,'Color',[1 1 1]);

axis([1 4 10 10000]);
hold off;

% MAGLITE
load('data/maglite_insert_map_reduce_hist.mat');
par = [ 1 2 4 8 16 32 ];
subplot(131)
set(gca,'YScale','log');
set(gca,'Xtick',1:6);
set(gca,'XTickLabel',par);
set(gca,'FontSize',15);
grid on;
hold on;

title('UltraSPARC T2');
xlabel('Number of CPUs');
ylabel('Execution Time [ms]');

plotScaling(1:length(par),ltqreduce,'--sq',cols(1,:));
plotScaling(1:length(par),slfpreduce,'-x',cols(3,:));
plotScaling(1:length(par),mlfpreduce,'-d',cols(4,:));
l1 = legend('Java LTQ','SingleLane FlowPool','MultiLane FlowPool','Location','SouthWest');
set(l1,'FontSize',12,'Color',[1 1 1]);

axis([1 6 100 10000]);
hold off;
