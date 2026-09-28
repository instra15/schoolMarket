package com.schoolMarket.service;

import com.schoolMarket.common.PageResponse;
import com.schoolMarket.common.Response;
import com.schoolMarket.dto.ProductCreateDTO;
import com.schoolMarket.entity.Product;
import com.schoolMarket.vo.ProductVO;

public interface ProductService {

    Response<Void> postProduct(ProductCreateDTO productCreateDTO);

    Response<Void> changeProductInfo(Long id,ProductCreateDTO productCreateDTO);

    Response<PageResponse<ProductVO>> findProduct(int Pageindex, int pageNum);

    Response<ProductVO> findProductById(Long id);
}
