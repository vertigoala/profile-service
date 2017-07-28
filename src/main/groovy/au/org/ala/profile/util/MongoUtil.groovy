package au.org.ala.profile.util

import groovy.transform.CompileStatic
import org.bson.Document
import org.bson.conversions.Bson

class MongoUtil {

    @CompileStatic
    static List<Bson> toBson(List pipeline) {
        List<Bson> newPipeline = new ArrayList<Bson>()
//        if (multiTenancyMode == MultiTenancyMode.DISCRIMINATOR) {
//            newPipeline.add(
//                    Filters.eq(MappingUtils.getTargetKey(persistentEntity.tenantId), Tenants.currentId((Class<Datastore>) datastore.getClass()))
//            )
//        }
        for (o in pipeline) {
            if (o instanceof Bson) {
                newPipeline << (Bson)o
            } else if (o instanceof Map) {
                newPipeline << new Document((Map) o)
            }
        }
        newPipeline
    }

}
