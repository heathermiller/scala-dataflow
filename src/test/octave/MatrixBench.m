a = 1:1000;
b = [1:1000]';
c = [1:1000]';

[ A, B ] = meshgrid(a,b);

res = (A * B) * c;
res' * res