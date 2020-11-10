clear;
T = readtable('TDM.csv', 'Format', 'auto');

%initialization

n = 16; %number of neurons

X = T{1:16, 2:695};
W = rand(694, n);

Z = zeros(16, 694);
V = zeros(694, n);

alpha = 0.1;

%normalization

for i = 1:size(W,2)
    for j = 1:size(W,1)
        V(j, i) = W(j, i)/norm(W(:, i));
    end
end

for i = 1:size(X,1)
    for j = 1:size(X,2)
        Z(i, j) = X(i, j)/norm(X(i, :));
    end
end

for k = 1:1000
    
    NET = Z*V;
    M = zeros(16, 1);
    I = zeros(16, 1);
    
    for i  = 1:size(NET, 1)
        [M(i, 1),I(i, 1)] = max(NET(i, :));
    end
    
    for i = 1:size(I, 1)
        W(:, I(i,1)) = V(:, I(i,1)) + alpha*Z(i, :).';
        V(:, I(i,1)) = W(:, I(i,1))/norm(W(:, I(i,1)));
    end
end

NET = Z*V;
M = zeros(16, 1);
I = zeros(16, 1);

for i  = 1:size(NET, 1)
    [M(i, 1),I(i, 1)] = max(NET(i, :));
end


