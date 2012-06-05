close all;

load('data/lampmac_insert_map_reduce_hist.mat');

grays = [0 0 0];
par = [1 2 4 8];

figure('units','pixels','Position',[1 1 1440 400]);

% INSERT
subplot(131)
set(gca,'YScale','log');
set(gca,'Xtick',1:4);
set(gca,'XTickLabel',par);
set(gca,'FontName','Times New Roman','FontSize',20);
set(gcf, 'Color', 'none');
set(gca, 'Color', 'none');
grid on;
hold on;

title('Insert');
ylabel('Execution Time [ms]');
plotScaling(1:length(par),ltqinsert,'--sq',grays+0.3); % ltq insert
plotScaling(1:length(par),clqinsert,'-o',grays+0.7); % clq insert
plotScaling(1:length(par),slfpinsert,'-x',grays+0.5); % slfp insert
plotScaling(1:length(par),mlfpinsert,'-d',grays); % mlfp insert
l1 = legend('Java LTQ','Java CLQ','SingleLane FlowPool','MultiLane FlowPool','Location','NorthEast');
set(l1,'FontSize',12,'Color',[1 1 1]);

axis([1 4 10 10000]);
hold off;
 
% MAP
subplot(132)
set(gca,'YScale','log');
set(gca,'Xtick',1:4);
set(gca,'XTickLabel',par);
set(gca,'FontName','Times New Roman','FontSize',20);
set(gcf, 'Color', 'none');
set(gca, 'Color', 'none');
grid on;
hold on;

title('Map');
xlabel('Number of CPUs');
plotScaling(1:length(par),ltqmap,'--sq',grays+0.3); % ltq map
plotScaling(1:length(par),slfpmap,'-x',grays+0.5); % slfp map
plotScaling(1:length(par),mlfpmap,'-d',grays); % mlfp map
l2 = legend('Java LTQ','SingleLane FlowPool','MultiLane FlowPool','Location','NorthEast');
set(l2,'FontSize',12,'Color',[1 1 1]);

axis([1 4 10 10000]);
hold off;

% REDUCE
subplot(133)
set(gca,'YScale','log');
set(gca,'Xtick',1:4);
set(gca,'XTickLabel',par);
set(gca,'FontName','Times New Roman','FontSize',20);
set(gcf, 'Color', 'none');
set(gca, 'Color', 'none');
grid on;
hold on;

title('Reduce');
plotScaling(1:length(par),ltqreduce,'--sq',grays+0.3); % ltq reduce
plotScaling(1:length(par),slfpreduce,'-x',grays+0.5); % slfp reduce
plotScaling(1:length(par),mlfpreduce,'-d',grays); % mlfp reduce
l3 = legend('Java LTQ','SingleLane FlowPool','MultiLane FlowPool','Location','NorthEast');
set(l3,'FontSize',12,'Color',[1 1 1]);

axis([1 4 10 10000]);
hold off;