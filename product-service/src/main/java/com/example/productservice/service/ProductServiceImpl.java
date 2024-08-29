package com.example.productservice.service;

import com.example.productservice.core.exception.CustomException;
import com.example.productservice.dto.ProductResponseDto;
import com.example.productservice.entity.Product;
import com.example.productservice.dto.ProductDto;
import com.example.productservice.entity.ProductStatus;
import com.example.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Transactional(readOnly = true) // 읽기 전용
@RequiredArgsConstructor
@Service
public class ProductServiceImpl implements ProductService {

    private final RedisTemplate<String, Integer> redisTemplate;

    private final ProductRepository productRepository;

    private final RedissonClient redissonClient;

    private static final String PRODUCT_KEY_PREFIX = "product:stock:";

    @Override
    public Page<ProductDto> findAll(Pageable pageable) {

        Page<ProductDto> productsDto = findByStatusNot(pageable);

        System.out.println();

        if(productsDto.isEmpty()){
            throw new CustomException("해당 상품이 비어있습니다!");
        }

        return productsDto;
    }

    private Page<ProductDto> findByStatusNot(Pageable pageable){
        Page<Product> products = productRepository.findByStatusNot(ProductStatus.DISCONTINUED, pageable);
        return products.map(ProductDto::new);
    }

    @Override
    public ProductDto findById(Long id) {
        Product product = productRepository.findById(id).orElseThrow(
                () -> new CustomException("해당 상품을 찾을수 없습니다")
        );

        return new ProductDto(product);
    }

    @Override
    public ProductResponseDto findByIdStatusProduct(Long id) {
        Product product = productRepository.findById(id).orElseThrow(
                () -> new CustomException("해당 상품을 찾을수 없습니다.")
        );

        if(product.getProductStatus() == ProductStatus.AVAILABLE){
            return new ProductResponseDto(product.getId(), product.getName(), product.getPrice(), product.getStock());

        }else {
            throw new CustomException("해당 상품은 현재 구매할수 없습니다.");
        }
    }

    @Override
    public ProductResponseDto getProductId(Long id) {
        Product product = productRepository.findById(id).orElseThrow(
                () -> new CustomException("해당 상품을 찾을수 없습니다.")
        );
        return new ProductResponseDto(product.getId(), product.getName(), product.getPrice(), product.getStock());
    }

    @Override
    public void decreaseStock(Long productId, int count) {
        String redisKey = PRODUCT_KEY_PREFIX + productId.toString();

        RLock lock = redissonClient.getLock("lock:" + redisKey);

        lock.lock(); // 락을 걸어 동시성 문제를 방지

        try {
            // Redis에서 현재 재고 조회
            Integer currentStock = redisTemplate.opsForValue().get(redisKey);

            System.out.println(currentStock);

            if (currentStock == null) {
                // Redis에 재고가 없으면 데이터베이스에서 조회 후 Redis에 캐시
                Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new RuntimeException("제품을 찾을 수 없습니다."));
                currentStock = product.getStock();
                redisTemplate.opsForValue().set(redisKey, currentStock);
            }

            if (currentStock < count) {
                throw new RuntimeException("재고가 부족합니다.");
            }

            // Redis에서 재고 감소
            redisTemplate.opsForValue().set(redisKey, currentStock - count);

        }finally {
            lock.unlock();
        }
    }

    // 데이터베이스에서 재고 감소
    @Async
    @Override
    public void asyncBatchUpdateStock(Long productId, int count) {
        updateStockBatch(productId, count);
    }


    @Transactional
    public void updateStockBatch(Long productId, int count) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("제품을 찾을 수 없습니다. 제품 ID: " + productId));

        // 재고 감소
        int newStock = product.getStock() - count;
        if (newStock < 0) {
            throw new RuntimeException("재고가 부족합니다. 제품 ID: " + productId);
        }
        product.setStock(newStock);

        productRepository.save(product);

    }


    @Override
    @Transactional
    public void increaseStock(Long productId, int count) {
        String redisKey = PRODUCT_KEY_PREFIX + productId.toString();

        // Redis에서 현재 재고 조회
        Integer currentStock = redisTemplate.opsForValue().get(redisKey);

        if (currentStock == null) {
            // Redis에 재고가 없으면 데이터베이스에서 조회 후 Redis에 캐시
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("제품을 찾을 수 없습니다."));
            currentStock = product.getStock();
            redisTemplate.opsForValue().set(redisKey, currentStock);
        }

        // 데이터베이스에서 재고 증가
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("제품을 찾을 수 없습니다."));
        product.setStock(product.getStock() + count);
        productRepository.save(product);

        // Redis에서 재고 증가
        redisTemplate.opsForValue().set(redisKey, currentStock + count);
    }

    @Override
    public int getStock(Long productId){
        String redisKey = PRODUCT_KEY_PREFIX + productId.toString();

        // 레디스에서 정보 조회
        Integer stock = redisTemplate.opsForValue().get(redisKey);


        if(stock == null){
            // 레디스에 재고 정보 캐싱
            Product product = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("제품을 찾을 수 없습니다."));
            stock = product.getStock();

            // 레디스에 재고 정보 캐싱 (키값, 재고)
            redisTemplate.opsForValue().set(redisKey,stock);
        }
        System.out.println(stock);
        return stock;
    }
}

