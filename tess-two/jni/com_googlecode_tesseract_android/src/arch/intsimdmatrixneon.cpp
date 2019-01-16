///////////////////////////////////////////////////////////////////////
// File:        intsindmatrixneon.cpp
// Description: NEON implementation of 8-bit int SIMD matrix multiply.
// Author:      Liu Shouyong,ZhangQiang
// Created:     2018/11/28
//
// (C) Copyright 2017, Google Inc.
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

#include "intsimdmatrixneon.h"

#include <cstdint>
#include <vector>
#include "dotproductneon.h"

namespace tesseract
{

// Computes part of matrix.vector v = Wu. Computes 1 result.
static void PartialMatrixDotVector2(const int8_t *wi, const double *scales,
                                    const int8_t *u, int num_in, int num_out,
                                    double *v)
{
  int total = IntDotProductNEON(u, wi, num_in);
  // Add in the bias and correct for integer values.
  *v = (static_cast<double>(total) / INT8_MAX + wi[num_in]) * *scales;
}

IntSimdMatrixNEON::IntSimdMatrixNEON()
{
  partial_funcs_ = {PartialMatrixDotVector2};
}

} // namespace tesseract.
