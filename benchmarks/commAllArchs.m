close all;

load('data/all_comm.mat');

grays = [0 0 0];
figure('units','pixels','Position',[1 1 1440 400]);

% MAGLITE
subplot(131)
sz = [2000000 4000000 12000000 25000000 50000000 100000000 200000000 400000000 800000000];
% set(gca,'YScale','log');
% set(gca,'Xtick',1:length(sz));
set(gca,'XTickLabel',sz);
set(gca,'FontName','Times New Roman','FontSize',20);
set(gcf, 'Color', 'none');
set(gca, 'Color', 'none');
set(gca,'XGrid','on','YGrid','on');
hold on;

% title('Communication/Gargbage Collection');
xlabel('Number of Elements');
ylabel('Execution Time [ms]');
plotSize(1:length(sz),magliteltqcomm,'--sq',grays+0.3); % ltq comm
plotSize(1:length(sz),maglitemlfpcomm,'-d',grays); % mlfp comm
l1 = legend('Java LTQ','MultiLane FlowPool','Location','NorthWest');
set(l1,'FontSize',12,'Color',[1 1 1]);

axis([1 length(sz) 1 340000]);
hold off;
 
% LAMPMAC
subplot(132)
sz = [2000000 4000000 12000000 25000000 50000000 100000000 200000000 400000000 800000000];
% set(gca,'YScale','log');
% set(gca,'Xtick',1:length(sz));
set(gca,'XTickLabel',sz);
set(gca,'FontName','Times New Roman','FontSize',20);
set(gcf, 'Color', 'none');
set(gca, 'Color', 'none');
set(gca,'XGrid','on','YGrid','on');
hold on;

% title('Communication/Gargbage Collection');
xlabel('Number of Elements');
plotSize(1:length(sz),lampmacltqcomm,'--sq',grays+0.3); % ltq comm
plotSize(1:length(sz),lampmacmlfpcomm,'-d',grays); % mlfp comm
l2 = legend('Java LTQ','MultiLane FlowPool','Location','NorthWest');
set(l2,'FontSize',12,'Color',[1 1 1]);

axis([1 length(sz) 1 340000]);
hold off;

% WOLF
subplot(133)
sz = [25000000 50000000 100000000 200000000 400000000];
% set(gca,'YScale','log');
% set(gca,'Xtick',1:length(sz));
set(gca,'XTickLabel',sz);
set(gca,'FontName','Times New Roman','FontSize',20);
set(gcf, 'Color', 'none');
set(gca, 'Color', 'none');
set(gca,'XGrid','on','YGrid','on');
hold on;

% title('Communication/Gargbage Collection');
xlabel('Number of Elements');
plotSize(1:2,wolfltqcomm,'--sq',grays+0.3); % ltq comm
plotSize(1:length(sz),wolfmlfpcomm,'-d',grays); % mlfp comm
l3 = legend('Java LTQ','MultiLane FlowPool','Location','NorthWest');
set(l3,'FontSize',12,'Color',[1 1 1]);

axis([1 length(sz) 1 340000]);
hold off;
