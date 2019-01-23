///////////////////////////////////////////////////////////////////////
// File:        dotproductneon.cpp
// Description: Architecture-specific dot-product function.
// Author:      Liu Shouyong,ZhangQiang
// Created:     2018/11/28
//
// (C) Copyright 2018, ZSmarter Technology Co., Ltd.
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
///////////////////////////////////////////////////////////////////////

#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <arm_neon.h>
#include "dotproductneon.h"

namespace tesseract
{
double DotProductNEON(const double *u, const double *v, int n)
{
    int max_offset = n - 4;
    int offset = 0;
    double result = 0.0;
    // Accumulate a set of 2 sums in sum, by loading pairs of 4 values from u and
    // v, and multiplying them together in parallel.
    double zero[] = {0.0, 0.0, 0.0, 0.0};

    float64x1x4_t sum = vld4_dup_f64(zero);

    if (offset <= max_offset)
    {
        offset = 4;
        // Aligned load is reputedly faster but requires 32 byte aligned input.
        // Use unaligned load.
        float64x1x4_t floats1 = vld4_f64(u);
        float64x1x4_t floats2 = vld4_f64(v);
        for (int i = 0; i < 4; i++)
            sum.val[i] = vmul_f64(floats1.val[i], floats2.val[i]);

        while (offset <= max_offset)
        {
            float64x1x4_t product = vld4_dup_f64(zero);
            floats1 = vld4_f64(u + offset);
            floats2 = vld4_f64(v + offset);
            for (int i = 0; i < 4; i++)
                product.val[i] = vmul_f64(floats1.val[i], floats2.val[i]);

            for (int i = 0; i < 4; i++)
                sum.val[i] = vadd_f64(sum.val[i], product.val[i]);

            offset += 4;
        }
    }

    for (int i = 0; i < 4; i++)
        result += sum.val[i][0];
    while (offset < n)
    {
        result += u[offset] * v[offset];
        ++offset;
    }

    return result;
}

int32_t IntDotProductNEON(const int8_t *u, const int8_t *v, int n)
{
    int32_t max_offset = n - 8;
    int32_t offset = 0;
    int32x4_t sum = {0, 0, 0, 0};
    int32x4_t sum_temp = {0, 0, 0, 0};
    int32x4x2_t temp;

    if (offset <= max_offset)
    {
        offset = 8;
        int16x8_t ints1 = vmovl_s8(vld1_s8(u));
        int16x8_t ints2 = vmovl_s8(vld1_s8(v));

        temp.val[0] = vmull_s16(vget_low_s16(ints1), vget_low_s16(ints2));
        temp.val[1] = vmull_s16(vget_high_s16(ints1), vget_high_s16(ints2));

        for (int i = 0; i < 2; i++)
        {
            sum[i * 2] = vaddv_s32(vget_low_s32(temp.val[i]));
            sum[i * 2 + 1] = vaddv_s32(vget_high_s32(temp.val[i]));
        }

        while (offset <= max_offset)
        {

            int16x8_t ints1 = vmovl_s8(vld1_s8(u + offset));
            int16x8_t ints2 = vmovl_s8(vld1_s8(v + offset));
            offset += 8;
            temp.val[0] = vmull_s16(vget_low_s16(ints1), vget_low_s16(ints2));
            temp.val[1] = vmull_s16(vget_high_s16(ints1), vget_high_s16(ints2));
            int32x4_t temp1 = {0, 0, 0, 0};
            for (int i = 0; i < 2; i++)
            {
                temp1[i * 2] = vaddv_s32(vget_low_s32(temp.val[i]));
                temp1[i * 2 + 1] = vaddv_s32(vget_high_s32(temp.val[i]));
            }

            sum = vpaddq_s32(sum, temp1);
        }
    }

    sum_temp = sum;
    for (int j = 0; j < 2; j++)
    {
        for (int i = 0; i < 2; i++)
        {
            sum[2 * i] = vaddv_s32(vget_low_s32(sum_temp));
            sum[2 * i + 1] = vaddv_s32(vget_high_s32(sum_temp));
        }
        sum_temp = sum;
    }

    int32_t result = vdups_laneq_s32(sum, 0);
    while (offset < n)
    {
        result += u[offset] * v[offset];
        ++offset;
    }
    return result;
}

} // namespace tesseract.