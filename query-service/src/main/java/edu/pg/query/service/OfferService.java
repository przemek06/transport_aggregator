package edu.pg.query.service;

import edu.pg.query.client.RPCClient;
import edu.pg.query.dto.OfferDto;
import edu.pg.query.dto.QueryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OfferService {

    private final RPCClient rpcClient;

    public Flux<List<OfferDto>> getOffers(String src, String dest, Date time, Double maxCost) {
        QueryDto query = new QueryDto(src, dest, time, maxCost);
        return rpcClient.request(query);
    }
}
