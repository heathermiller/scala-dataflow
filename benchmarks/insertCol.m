close all;

cols = [ 1 0 0 ; 0 .7 0 ; 0 0 1 ; .6 0 .8 ];
par = [1 2 4 8];



figure('units','pixels','Position',[1 1 1440 600]);

% WOLF
load('data/wolf_insert_map_reduce_hist.mat');
subplot(131)
set(gca,'YScale','log');
set(gca,'Xtick',1:4);
set(gca,'XTickLabel',par);
set(gca,'FontSize',20);
grid on;
hold on;

title('32-core Xeon');
ylabel('Execution Time [ms]');
plotScaling(1:length(par),ltqinsert,'--sq',cols(1,:)); % ltq insert
plotScaling(1:length(par),clqinsert,'-o',cols(2,:)); % clq insert
plotScaling(1:length(par),slfpinsert,'-x',cols(3,:)); % slfp insert
plotScaling(1:length(par),mlfpinsert,'-d',cols(4,:)); % mlfp insert
l1 = legend('Java LTQ','Java CLQ','SingleLane FlowPool','MultiLane FlowPool','Location','SouthEast');
set(l1,'FontSize',12,'Color',[1 1 1]);

axis([1 4 10 10000]);
hold off;

% LAMPMAC
load('data/lampmac_insert_map_reduce_hist.mat');
subplot(132)
set(gca,'YScale','log');
set(gca,'Xtick',1:4);
set(gca,'XTickLabel',par);
set(gca,'FontSize',20);
grid on;
hold on;

title('4-core i7');
ylabel('Execution Time [ms]');
plotScaling(1:length(par),ltqinsert,'--sq',cols(1,:)); % ltq insert
plotScaling(1:length(par),clqinsert,'-o',cols(2,:)); % clq insert
plotScaling(1:length(par),slfpinsert,'-x',cols(3,:)); % slfp insert
plotScaling(1:length(par),mlfpinsert,'-d',cols(4,:)); % mlfp insert
l1 = legend('Java LTQ','Java CLQ','SingleLane FlowPool','MultiLane FlowPool','Location','NorthEast');
set(l1,'FontSize',12,'Color',[1 1 1]);

axis([1 4 10 10000]);
hold off;

% MAGLITE
load('data/maglite_insert_map_reduce_hist.mat');
par = [ 1 2 4 8 16 32 ];
subplot(133)
set(gca,'YScale','log');
set(gca,'Xtick',1:6);
set(gca,'XTickLabel',par);
set(gca,'FontSize',20);
grid on;
hold on;

title('UltraSPARC T2');
ylabel('Execution Time [ms]');
plotScaling(1:length(par),ltqinsert,'--sq',cols(1,:)); % ltq insert
plotScaling(1:length(par),clqinsert,'-o',cols(2,:)); % clq insert
plotScaling(1:length(par)-1,slfpinsert,'-x',cols(3,:)); % slfp insert
plotScaling(1:length(par),mlfpinsert,'-d',cols(4,:)); % mlfp insert
l1 = legend('Java LTQ','Java CLQ','SingleLane FlowPool','MultiLane FlowPool','Location','SouthWest');
set(l1,'FontSize',12,'Color',[1 1 1]);

axis([1 6 100 10000]);
hold off;

