close all;

load('data/maglite_hist.mat');

par = [1 2 4 8 16 32];
grays = [0 0 0];

figure;

set(gca,'YScale','log');
set(gca,'Xtick',1:6);
set(gca,'XTickLabel',par);
set(gca,'FontName','Times New Roman','FontSize',20);
grid on;
hold on;

% lampmac (BLACK)
title('Histogram');
ylabel('Execution Time [ms]');
plotScaling(1:length(par),ltqhist,'--sq',grays+0.3); % ltq hist
plotScaling(1:length(par),slfphist,'-x',grays); % slfp hist
plotScaling(1:length(par),mlfphist,'-o',grays+0.7); % mlfp hist
l1 = legend('Java LTQ','SingleLane FlowPool','MultiLane FlowPool','Location','SouthEast');
set(l1,'FontSize',12);

hold off;