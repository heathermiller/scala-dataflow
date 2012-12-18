n = 500;
a = 1:n;
b = [1:n]';
c = [1:n]';

[ A, B ] = meshgrid(a,b);

res = (A * B) * c;
res' * res
