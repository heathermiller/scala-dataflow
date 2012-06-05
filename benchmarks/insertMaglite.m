close all;

grays = [0 0 0];
par = [1 2 4 8 16];

figure;

subplot(121)
set(gca,'YScale','log');
set(gca,'Xtick',1:6);
set(gca,'XTickLabel',par);
set(gca,'FontName','Times New Roman','FontSize',20);
grid on;
hold on;

% lampmac (BLACK)
title('Map');
ylabel('Execution Time [ms]');
plotScaling(1:length(par),ltqinsert,'--sq',grays+0.3); % ltq insert
plotScaling(1:length(par),clqinsert,'-o',grays+0.7); % clq insert
plotScaling(1:length(par),clqinsert,'-x',grays+0.7); % slfp insert
plotScaling(1:length(par),clqinsert,'-tr',grays+0.7); % mlfp insert
l1 = legend('Java LTQ','Java CLQ','SingleLane FlowPool','MultiLane FlowPool','Location','SouthEast');
set(l1,'FontSize',11);

hold off;

subplot(122)
set(gca,'YScale','log');
set(gca,'Xtick',1:6);
set(gca,'XTickLabel',par);
set(gca,'FontName','Times New Roman','FontSize',20);
grid on;
hold on;

% maglite (MED GRAY)
title('Reduce');
plotScaling(1:length(histdata.maglitepar),histdata.maglite{1},'--sq',grays+0.3); % ltq
plotScaling(1:length(histdata.maglitepar),histdata.maglite{3},'-o',grays+0.7); % mlfp
plotScaling(1:length(histdata.maglitepar),histdata.maglite{5},'-x',grays); % slfp
l2 = legend('Java LTQ','MultiLane FlowPool','SingleLane FlowPool','Location','SouthEast');
set(l2,'FontSize',12);

hold off;
