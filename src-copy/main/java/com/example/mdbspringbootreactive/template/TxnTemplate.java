package com.example.mdbspringbootreactive.template;

import com.example.mdbspringbootreactive.enumeration.ErrorReason;
import com.example.mdbspringbootreactive.enumeration.TxnStatus;
import com.example.mdbspringbootreactive.model.Txn;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.core.query.Update.update;

@Service
public class TxnTemplate {

    private final ReactiveMongoTemplate template;

    public TxnTemplate(ReactiveMongoTemplate template) {
        this.template = template;
    }

    public Mono<Txn> save(Txn txn) {
        return template.save(txn);
    }

    public Mono<Txn> findAndUpdateStatusById(String id, TxnStatus status) {
        Query query = query(where("_id").is(id));
        Update update = update("status", status);
        FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(true);
        return template.findAndModify(query, update, options, Txn.class);
    }

    public Mono<Txn> findAndUpdateStatusById(String id, TxnStatus status, ErrorReason errorReason) {
        Query query = query(where("_id").is(id));
        Update update = update("status", status).set("errorReason", errorReason);
        FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(true);
        return template.findAndModify(query, update, options, Txn.class);
    }
}
