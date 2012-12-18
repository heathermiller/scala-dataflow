#include <stddef.h>
#include <stdlib.h>

#define SIZE 1000

void mmul(double const * const a,
          double const * const b,
          const size_t l,
          const size_t m,
          const size_t n,
          double * const res);

int main(int argc, char** argv) {
    
    double *A = calloc(SIZE * SIZE, sizeof(double));
    double *B = calloc(SIZE * SIZE, sizeof(double));
    double *C = calloc(SIZE * SIZE, sizeof(double));
    double *v1 = calloc(SIZE, sizeof(double));
    double *vr = calloc(SIZE, sizeof(double)); 
    double r;

    mmul(A, B, SIZE, SIZE, SIZE, C); // A * B
    mmul(C, v1, SIZE, SIZE, 1, vr);  // (A * B) * c
    mmul(v1, v1, 1, SIZE, 1, &r);

}

void mmul(double const * const a,
          double const * const b,
          const size_t l,
          const size_t m,
          const size_t n,
          double * const res) {

    size_t li, ni, mi;

    for (li = 0; li < l; li++) {
        for (ni = 0; ni < n; ni++) {
            double acc = 0;
            for (mi = 0; mi < m; mi++)
                acc += a[li * m + mi] * b[mi * n + ni];
            res[li * n + ni] = acc;
        }
    }

}
