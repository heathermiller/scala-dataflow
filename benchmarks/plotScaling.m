function avgs = plotScaling(par,data,line,color)

avgs= [];
[m,n] = size(data);
if (length(par) ~= m)
    error('Number of cores in par needs to be the same as the number of rows of data');
end

for i=1:m
    avgRunningTime = sum(data(i,:))/n;
    avgs = [avgs avgRunningTime];
end
semilogy(par,avgs,line,'Color',color,'LineWidth',2,...
    'MarkerSize',10,'MarkerEdgeColor','k','MarkerFaceColor',color);

end