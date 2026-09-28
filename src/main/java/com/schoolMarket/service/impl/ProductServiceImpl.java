package com.schoolMarket.service.impl;

import com.schoolMarket.common.PageResponse;
import com.schoolMarket.common.Response;
import com.schoolMarket.dto.ProductCreateDTO;
import com.schoolMarket.entity.Product;
import com.schoolMarket.exception.BusinessException;
import com.schoolMarket.mapper.ProductMapper;
import com.schoolMarket.service.ProductService;
import com.schoolMarket.vo.ProductVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Override
    public Response<Void> postProduct(ProductCreateDTO productCreateDTO) {
        productMapper.insert(productCreateDTO);
        return Response.success(null,"Successfully add product: " + productCreateDTO);
    }

    @Override
    public Response<Void> changeProductInfo(Long id ,ProductCreateDTO productCreateDTO) {
        Product product=productMapper.findById(id);
        if (product==null)
        {
            throw new BusinessException("Product does not exist.");
        }
        productMapper.updateProduct(id,productCreateDTO);
        return Response.success(null,"Successfully update product: " + productCreateDTO);
    }

    @Override
    public Response<PageResponse<ProductVO>> findProduct(int Pageindex, int pageNum) {
        return null;
    }

    @Override
    public Response<ProductVO> findProductById(Long id) {
        return null;
    }
}
