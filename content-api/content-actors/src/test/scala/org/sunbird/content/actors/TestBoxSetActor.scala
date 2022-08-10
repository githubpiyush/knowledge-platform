package org.sunbird.content.actors

import java.util
import akka.actor.Props
import org.scalamock.scalatest.MockFactory
import org.sunbird.common.HttpUtil
import org.sunbird.common.dto.{Property, Request, Response, ResponseHandler, ResponseParams}
import org.sunbird.common.exception.ResponseCode
import org.sunbird.graph.dac.model.{Node, Relation, SearchCriteria}
import org.sunbird.graph.nodes.DataNode.getRelationMap
import org.sunbird.graph.utils.ScalaJsonUtils
import org.sunbird.graph.{GraphService, OntologyEngineContext}
import org.sunbird.kafka.client.KafkaClient

import java.util
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TestBoxSetActor extends BaseSpec with MockFactory{

    "boxSetActor" should "return failed response for 'unknown' operation" in {
      implicit val oec: OntologyEngineContext = new OntologyEngineContext
      testUnknownOperation(Props(new BoxSetActor()), getBoxSetRequest())
    }


  it should "return success response for 'addBook'" in {
    implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
    val graphDB = mock[GraphService]
    (oec.graphService _).expects().returns(graphDB).anyNumberOfTimes()
    val dataNode = Some(new util.HashMap[String, AnyRef](){
      put("lastStatusChangedOn","2022-08-09T02:14:27.304+0530")
      put("cost",(480.0).asInstanceOf[AnyRef])
      put("year","2016")
      put("bookCount",2.asInstanceOf[AnyRef])
      put("author","Piyush")
      put("channel","{{channel_id}}")
      put("weight",2.asInstanceOf[AnyRef])
      put("title","Coding Books")
      put("version",1.asInstanceOf[AnyRef])
      put("createdOn","2022-08-09T02:14:27.304+0530")
      put("versionKey","1659991467304")
      put("lastUpdatedOn","2022-08-09T02:14:27.304+0530")
      put("status","in-stock")
      put("bookList", """["do_11359864963937075212","do_11359864946959155211"]""")
    })
    val node = getNode("BoxSet", dataNode)

    //      node.setExternalData(mapAsJavaMap(Map(
    //        "bookList" -> "[do_11359864963937075212, do_11359864946959155211]")))
    //      node.setExternalData(new util.HashMap[String, AnyRef](){
    //        put("bookList",List("do_11359864963937075212", "do_11359864946959155211"))
    //      })
    val dataBook = Some(new util.HashMap[String, AnyRef](){
      put("pageCount",380.asInstanceOf[AnyRef])
      put("lastStatusChangedOn","2022-08-09T02:13:40.494+0530")
      put("cost",(450.0).asInstanceOf[AnyRef])
      put("paperQuality","Good")
      put("visibility","default")
      put("channel","{{channel_id}}")
      put("title","Programming with Pyhton")
      put("type","handcover")
      put("version",1.asInstanceOf[AnyRef])
      put("createdOn","2022-08-09T02:13:40.494+0530")
      put("versionKey","1659991420494")
      put("subTitle","Learn Python")
      put("lastUpdatedOn","2022-08-09T02:13:40.494+0530")
    })
    val nodeBook = getNode("Book", dataBook)
    nodeBook.setIdentifier("do_11359864970015539213")

    val nodes: util.List[Node] = getCategoryNode()
    val updateParams = new ResponseParams()
    updateParams.setStatus("successful")
    val responseRead = new Response()
    responseRead.setParams(updateParams)
    responseRead.putAll(new util.HashMap[String, Object](){
      put("summary","Standard books for coding")
      put("bookList","""["do_11359864963937075212","do_11359864946959155211"]""")
    })


    val propBoxset = new Property("versionKey", "1659991420494".asInstanceOf[Object])
    (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(node))
//    (graphDB.readExternalProps(_: Request, _: List[String])).expects(*, *).returns(Future(responseRead))
    (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(nodeBook))
    (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(nodeBook))
    (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(node))
    (graphDB.getNodeByUniqueIds(_: String, _: SearchCriteria)).expects(*, *).returns(Future(nodes))
    (graphDB.getNodeProperty(_: String, _: String, _: String)).expects(*, *, *).returns(Future(propBoxset))
    val node2 = getNode("BoxSet", Some(new util.HashMap[String, AnyRef](){
      put("lastStatusChangedOn","2022-08-09T02:14:27.304+0530")
      put("cost","(840.0)".asInstanceOf[AnyRef])
      put("year","2016")
      put("bookCount",3.asInstanceOf[AnyRef])
      put("author","Piyush")
      put("channel","{{channel_id}}")
      put("weight",2.asInstanceOf[AnyRef])
      put("title","Coding Books")
      put("version",1.asInstanceOf[AnyRef])
      put("createdOn","2022-08-09T02:14:27.304+0530")
      put("versionKey","1659991932681")
      put("lastUpdatedOn","2022-08-09T02:14:27.304+0530")
      put("status","in-stock")
      put("identifier","test_id")
    }))
    node2.setExternalData(mapAsJavaMap(Map(
      "bookList" -> "[do_11359864963937075212, do_11359864946959155211, do_11359864970015539213]")))
    val dataBook2 = Some(new util.HashMap[String, AnyRef](){
      put("pageCount",380.asInstanceOf[AnyRef])
      put("lastStatusChangedOn","2022-08-09T02:13:40.494+0530")
      put("cost",(450.0).asInstanceOf[AnyRef])
      put("paperQuality","Good")
      put("visibility","parent")
      put("channel","{{channel_id}}")
      put("title","Programming with Pyhton")
      put("type","handcover")
      put("version",1.asInstanceOf[AnyRef])
      put("createdOn","2022-08-09T02:13:40.494+0530")
      put("versionKey","1659991932681")
      put("subTitle","Learn Python")
      put("lastUpdatedOn","2022-08-09T02:13:40.494+0530")
    })
    val nodeBook2 = getNode("Book", dataBook2)
    nodeBook2.setIdentifier("do_11359864970015539213")

    val updateResponse = new Response()

    updateResponse.setParams(updateParams)
    (graphDB.upsertNode(_: String, _: Node, _: Request)).expects(*,*,*).returns(Future(node2))
//    (graphDB.upsertNode(_: String, _: Node, _: Request)).expects(*,*,*).returns(Future(nodeBook2))
    (graphDB.updateExternalProps(_: Request)).expects(*).returns(Future(updateResponse))
    val request = getBoxSetRequest()
    request.getContext.put("identifier", "test_id")
    request.getRequest.put("identifier", "test_id")
    request.putAll(mapAsJavaMap(Map(
      "versionKey" -> "1659991467304",
      "bookList" -> List("do_11359864970015539213").asJava
    )))
    request.setOperation("addBook")


    val response = callActor(request, Props(new BoxSetActor()))
    println(response.getParams,"xssssssssss))))))))))")
    assert("successful".equals(response.getParams.getStatus))
  }

  it should "return failed response for 'addBook'" in {
    implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
    val graphDB = mock[GraphService]
    (oec.graphService _).expects().returns(graphDB).anyNumberOfTimes()
    val dataNode = Some(new util.HashMap[String, AnyRef](){
      put("lastStatusChangedOn","2022-08-09T02:14:27.304+0530")
      put("cost",(480.0).asInstanceOf[AnyRef])
      put("year","2016")
      put("bookCount",2.asInstanceOf[AnyRef])
      put("author","Piyush")
      put("channel","{{channel_id}}")
      put("weight",2.asInstanceOf[AnyRef])
      put("title","Coding Books")
      put("version",1.asInstanceOf[AnyRef])
      put("createdOn","2022-08-09T02:14:27.304+0530")
      put("versionKey","1659991467304")
      put("lastUpdatedOn","2022-08-09T02:14:27.304+0530")
      put("status","in-stock")
      put("bookList", """["do_11359864963937075212","do_11359864946959155211"]""")
    })
    val node = getNode("BoxSet", dataNode)

    //      node.setExternalData(mapAsJavaMap(Map(
    //        "bookList" -> "[do_11359864963937075212, do_11359864946959155211]")))
    //      node.setExternalData(new util.HashMap[String, AnyRef](){
    //        put("bookList",List("do_11359864963937075212", "do_11359864946959155211"))
    //      })
    val dataBook = Some(new util.HashMap[String, AnyRef](){
      put("pageCount",380.asInstanceOf[AnyRef])
      put("lastStatusChangedOn","2022-08-09T02:13:40.494+0530")
      put("cost",(450.0).asInstanceOf[AnyRef])
      put("paperQuality","Good")
      put("visibility","parent")
      put("channel","{{channel_id}}")
      put("title","Programming with Pyhton")
      put("type","handcover")
      put("version",1.asInstanceOf[AnyRef])
      put("createdOn","2022-08-09T02:13:40.494+0530")
      put("versionKey","1659991420494")
      put("subTitle","Learn Python")
      put("lastUpdatedOn","2022-08-09T02:13:40.494+0530")
    })
    val nodeBook = getNode("Book", dataBook)
    nodeBook.setIdentifier("do_11359864970015539213")

    val nodes: util.List[Node] = getCategoryNode()
    val updateParams = new ResponseParams()
    updateParams.setStatus("successful")
    val responseRead = new Response()
    responseRead.setParams(updateParams)
    responseRead.putAll(new util.HashMap[String, Object](){
      put("summary","Standard books for coding")
      put("bookList","""["do_11359864963937075212","do_11359864946959155211"]""")
    })


    val propBoxset = new Property("versionKey", "1659991420494".asInstanceOf[Object])
    (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(node))
    //      (graphDB.readExternalProps(_: Request, _: List[String])).expects(*, *).returns(Future(responseRead))
    (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(nodeBook))
//    (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(nodeBook))
//    (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(node))
//    (graphDB.getNodeByUniqueIds(_: String, _: SearchCriteria)).expects(*, *).returns(Future(nodes))
//    (graphDB.getNodeProperty(_: String, _: String, _: String)).expects(*, *, *).returns(Future(propBoxset))
    val node2 = getNode("BoxSet", Some(new util.HashMap[String, AnyRef](){
      put("lastStatusChangedOn","2022-08-09T02:14:27.304+0530")
      put("cost","(840.0)".asInstanceOf[AnyRef])
      put("year","2016")
      put("bookCount",3.asInstanceOf[AnyRef])
      put("author","Piyush")
      put("channel","{{channel_id}}")
      put("weight",2.asInstanceOf[AnyRef])
      put("title","Coding Books")
      put("version",1.asInstanceOf[AnyRef])
      put("createdOn","2022-08-09T02:14:27.304+0530")
      put("versionKey","1659991932681")
      put("lastUpdatedOn","2022-08-09T02:14:27.304+0530")
      put("status","in-stock")
      put("identifier","test_id")
    }))
    node2.setExternalData(mapAsJavaMap(Map(
      "bookList" -> "[do_11359864963937075212, do_11359864946959155211, do_11359864970015539213]")))
    val dataBook2 = Some(new util.HashMap[String, AnyRef](){
      put("pageCount",380.asInstanceOf[AnyRef])
      put("lastStatusChangedOn","2022-08-09T02:13:40.494+0530")
      put("cost",(450.0).asInstanceOf[AnyRef])
      put("paperQuality","Good")
      put("visibility","parent")
      put("channel","{{channel_id}}")
      put("title","Programming with Pyhton")
      put("type","handcover")
      put("version",1.asInstanceOf[AnyRef])
      put("createdOn","2022-08-09T02:13:40.494+0530")
      put("versionKey","1659991932681")
      put("subTitle","Learn Python")
      put("lastUpdatedOn","2022-08-09T02:13:40.494+0530")
    })
    val nodeBook2 = getNode("Book", dataBook2)
    nodeBook2.setIdentifier("do_11359864970015539213")

    val updateResponse = new Response()

    updateResponse.setParams(updateParams)
//    (graphDB.upsertNode(_: String, _: Node, _: Request)).expects(*,*,*).returns(Future(node2))
    //      (graphDB.upsertNode(_: String, _: Node, _: Request)).expects(*,*,*).returns(Future(nodeBook2))
//    (graphDB.updateExternalProps(_: Request)).expects(*).returns(Future(updateResponse))
    val request = getBoxSetRequest()
    request.getContext.put("identifier", "test_id")
    request.getRequest.put("identifier", "test_id")
    request.putAll(mapAsJavaMap(Map(
      "versionKey" -> "1659991467304",
      "bookList" -> List("do_11359864970015539213").asJava
    )))
    request.setOperation("addBook")


    val response = callActor(request, Props(new BoxSetActor()))
    println(response.getParams,"xssssssssss))))))))))")
    assert("failed".equals(response.getParams.getStatus))
  }

  private def getBoxSetRequest(): Request = {
    val request = new Request()
    request.setContext(new java.util.HashMap[String, AnyRef]() {
      {
        put("graph_id", "domain")
        put("version", "1.0")
        put("objectType", "BoxSet")
        put("schemaName", "boxset")
      }
    })
    request.setObjectType("BoxSet")
    request
  }
}
