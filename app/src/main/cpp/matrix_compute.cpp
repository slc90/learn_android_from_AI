#include "matrix_compute.h"

#include <Eigen/Dense>
#include <chrono>
#include <android/log.h>

double runMatrixMultiply(int size) {
    auto start = std::chrono::steady_clock::now();

    Eigen::MatrixXd a =
            Eigen::MatrixXd::Random(size, size);

    Eigen::MatrixXd b =
            Eigen::MatrixXd::Random(size, size);

    Eigen::MatrixXd c = a * b;

    double result = c.sum();

    auto end = std::chrono::steady_clock::now();

    auto duration =
            std::chrono::duration_cast<std::chrono::milliseconds>(
                    end - start
            ).count();

    __android_log_print(
            ANDROID_LOG_INFO,
            "MatrixCompute",
            "size=%d, duration=%lld ms, result=%f",
            size,
            static_cast<long long>(duration),
            result
    );

    return result;
}