close all;

load('data/all_comm.mat');

cols = [ 1 0 0 ; 0 .7 0 ; 0 0 1 ; .6 0 .8 ];
figure('units','pixels','Position',[1 1 1440 600]);

% MAGLITE
subplot(131)
sz = [2000000 4000000 12000000 25000000 50000000 100000000 200000000 400000000 800000000];
set(gca,'XTickLabel',sz);
set(gca,'FontSize',15);
set(gca,'XGrid','on','YGrid','on');
hold on;

title('UltraSPARC T2');
xlabel('Number of Elements');
ylabel('Execution Time [ms]');
plotSize(1:length(sz),magliteltqcomm,'--sq',cols(1,:)); % ltq comm
plotSize(1:length(sz),maglitemlfpcomm,'-d',cols(4,:)); % mlfp comm
l1 = legend('Java LTQ','MultiLane FlowPool','Location','NorthWest');
set(l1,'FontSize',12,'Color',[1 1 1]);

axis([1 length(sz) 1 340000]);
hold off;
 
% LAMPMAC
subplot(132)
sz = [2000000 4000000 12000000 25000000 50000000 100000000 200000000 400000000 800000000];
set(gca,'XTickLabel',sz);
set(gca,'FontSize',15);
set(gca,'XGrid','on','YGrid','on');
hold on;

title('4-core i7');
xlabel('Number of Elements');
plotSize(1:length(sz),lampmacltqcomm,'--sq',cols(1,:)); % ltq comm
plotSize(1:length(sz),lampmacmlfpcomm,'-d',cols(4,:)); % mlfp comm
l2 = legend('Java LTQ','MultiLane FlowPool','Location','NorthWest');
set(l2,'FontSize',12,'Color',[1 1 1]);

axis([1 length(sz) 1 340000]);
hold off;

% WOLF
subplot(133)
sz = [25000000 50000000 100000000 200000000 400000000];
set(gca,'XTickLabel',sz);
set(gca,'FontSize',15);
set(gca,'XGrid','on','YGrid','on');
hold on;

title('32-core Xeon');
xlabel('Number of Elements');
plotSize(1:2,wolfltqcomm,'--sq',cols(1,:)); % ltq comm
plotSize(1:length(sz),wolfmlfpcomm,'-d',cols(4,:)); % mlfp comm
l3 = legend('Java LTQ','MultiLane FlowPool','Location','NorthWest');
set(l3,'FontSize',12,'Color',[1 1 1]);

axis([1 length(sz) 1 340000]);
hold off;
