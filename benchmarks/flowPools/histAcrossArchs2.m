close all;

load('data/all_hist.mat');

grays = [0 0 0];
figure('units','pixels','Position',[1 1 1440 400]);

% MAGLITE
subplot(131)
par = [1 2 4 8 16 32];
set(gca,'YScale','log');
set(gca,'Xtick',1:length(par));
set(gca,'XTickLabel',par);
set(gca,'FontName','Times New Roman','FontSize',20);
set(gcf, 'Color', 'none');
set(gca, 'Color', 'none');
grid on;
hold on;

% title('UltraSPARC Architecture');
xlabel('Number of CPUs');
ylabel('Execution Time [ms]');
plotScaling(1:length(par),magliteltqhist,'--sq',grays+0.3); % ltq hist
plotScaling(1:length(par),magliteslfphist,'-x',grays+0.5); % slfp hist
plotScaling(1:length(par),maglitemlfphist,'-d',grays); % mlfp hist
l1 = legend('Java LTQ','SingleLane FlowPool','MultiLane FlowPool','Location','NorthEast');
set(l1,'FontSize',12,'Color',[1 1 1]);

axis([1 6 100 100000]);
hold off;
 
% LAMPMAC
subplot(132)
par = [1 2 4 8];
set(gca,'YScale','log');
set(gca,'Xtick',1:4);
set(gca,'XTickLabel',par);
set(gca,'FontName','Times New Roman','FontSize',20);
set(gcf, 'Color', 'none');
set(gca, 'Color', 'none');
grid on;
hold on;

% title('Intel i7 Architecture');
xlabel('Number of CPUs');
plotScaling(1:length(par),lampmacltqhist,'--sq',grays+0.3); % ltq hist
plotScaling(1:length(par),lampmacslfphist,'-x',grays+0.5); % slfp hist
plotScaling(1:length(par),lampmacmlfphist,'-d',grays); % mlfp hist
l2 = legend('Java LTQ','SingleLane FlowPool','MultiLane FlowPool','Location','NorthEast');
set(l2,'FontSize',12,'Color',[1 1 1]);

axis([1 4 100 100000]);
hold off;

% WOLF
subplot(133)
par = [1 2 4 8];
set(gca,'YScale','log');
set(gca,'Xtick',1:4);
set(gca,'XTickLabel',par);
set(gca,'FontName','Times New Roman','FontSize',20);
set(gcf, 'Color', 'none');
set(gca, 'Color', 'none');
grid on;
hold on;

% title('Intel Xeon Architecture');
xlabel('Number of CPUs');
plotScaling(1:length(par),wolfltqhist,'--sq',grays+0.3); % ltq hist
plotScaling(1:length(par),wolfslfphist,'-x',grays+0.5); % slfp hist
plotScaling(1:length(par),wolfmlfphist,'-d',grays); % mlfp hist
l3 = legend('Java LTQ','SingleLane FlowPool','MultiLane FlowPool','Location','NorthEast');
set(l3,'FontSize',12,'Color',[1 1 1]);

axis([1 4 100 100000]);
hold off;