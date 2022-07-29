package org.sunbird.content.actors

import org.apache.commons.lang3.StringUtils
import org.apache.kafka.common.utils.Java
import org.sunbird.actor.core.BaseActor
import org.sunbird.common.dto.{Request, Response, ResponseHandler}
import org.sunbird.common.exception.ClientException
import org.sunbird.content.util.ContentConstants
import org.sunbird.graph.OntologyEngineContext
import org.sunbird.graph.nodes.DataNode
import org.sunbird.util.RequestUtil

import java.util
//import java.time.
import javax.inject.Inject
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class BoxSetActor @Inject()(implicit oec: OntologyEngineContext) extends BaseActor {

  implicit val ec: ExecutionContext = getContext().dispatcher

  override def onReceive(request: Request): Future[Response] = request.getOperation match {
    case "createBoxset" => create(request)
    //case "readBoxset" =>
    //case "updateBoxset" =>
    case _ => ERROR(request.getOperation)
  }
  def check_all_constraints_boxset(request: Request) = {
    var book_cost: Long = 0
    val bookList = request.getRequest.getOrDefault("bookList", new util.ArrayList[String]).asInstanceOf[util.List[String]]

    for (i <- 0 to bookList.size()-1) {
      var node_present = false

      val book_read = new Request(request)
      book_read.getContext.put("graph_id","domain")
      book_read.getContext.put("schemaName","book")
      book_read.getContext.put("objectType","Book")
      book_read.getContext.put("version","1.0")
      book_read.getContext.put("consumerId","X-Consumer-ID")

      val book_value = new util.HashMap[String, Object]()
      book_value.putAll(Map("identifier" -> bookList.get(i)).asJava)
      book_read.copyRequestValueObjects(book_value)

      println("Book read 1",book_read, book_read.getClass)
      println("Book read 2",book_read.getOperation)
      DataNode.read(book_read).foreach(node => {
        node_present = true
        val visibility: String = node.getMetadata.getOrDefault("visibility", "").asInstanceOf[String]
        val versionKey: String = node.getMetadata.getOrDefault("versionKey", "").asInstanceOf[String]

        if (StringUtils.equals(visibility, "parent"))
          throw new ClientException(ContentConstants.ERR_USED_BOOK_ID, s"This book's visibility is parent  : ${node.getIdentifier}.")

        book_cost += node.getMetadata.get("cost").asInstanceOf[Long]

        val book_update = new Request(request)
        book_update.getContext.put("graph_id","domain")
        book_update.getContext.put("schemaName","book")
        book_update.getContext.put("identifier",bookList.get(i))
        book_update.getContext.put("objectType","Book")
        book_update.getContext.put("version","1.0")
        book_update.getContext.put("consumerId","X-Consumer-ID")

        val book_update_value = new util.HashMap[String, Object]()
        book_update_value.putAll(Map("versionKey" -> versionKey,"identifier" -> bookList.get(i), "visibility" -> "parent").asJava)
        book_update.copyRequestValueObjects(book_update_value)

        println("Book update 1",book_update, book_update.getClass)
        println("Book update 2",book_update.getOperation)
        DataNode.update(book_update).map(node_1 => {
          ResponseHandler.OK.putAll(Map("identifier" -> node_1.getIdentifier.replace(".img", ""), "versionKey" -> node_1.getMetadata.get("versionKey")).asJava)
        })

        println(visibility.getClass, book_cost,book_cost.getClass, "+++++++++++++")
        println(node.getMetadata.get("cost"),node.getMetadata.get("visibility"))
        println(node.getMetadata, node.getExternalData, node.getIdentifier, node.getNode)
      })
      Thread.sleep(2000)
      if (!node_present)
        throw new ClientException(ContentConstants.ERR_NULL_BOOK_ID, s"Invalid Book ID  : ${bookList.get(i)}.")


    }
    Thread.sleep(7000) // wait for 7 seconds
//    val temp_obj = new util.HashMap[String, AnyRef]()
//    temp_obj.putAll(Map("cost" -> book_cost * 0.80 ,"bookCount" -> bookList.size()).asJava)
//    request.getRequest.put("cost", book_cost * 0.80)
//    request.getRequest.put("bookCount", bookList.size())

    if (bookList.size() < 3 )
      throw new ClientException(ContentConstants.ERR_BOXSET_BOOK_COUNT, "Less than two books or bookCount doesn't match with bookList size")
  }
  def create(request: Request): Future[Response] = {
    RequestUtil.restrictProperties(request)
    check_all_constraints_boxset(request)
//    var book_cost: Long = 0
//    val bookList = request.getRequest.getOrDefault("bookList", new util.ArrayList[String]).asInstanceOf[util.List[String]]
//
//    for (i <- 0 to bookList.size()-1) {
//      var node_present = false
//      val boxSet = new java.util.HashMap[String, Object]()
//      boxSet.putAll(Map("identifier" -> bookList.get(i), "mode" -> "read").asJava)
//
//      val boxset_req = new Request(request.getContext(), boxSet, "readNode", null)
//      boxset_req.getContext.put("objectType", "Book")
//      boxset_req.getContext.put("schemaName", "book")
//
//      println("Boxset 1",boxset_req, boxset_req.getParams, boxset_req.getClass,request.getContext)
//      println("Boxset 2",boxset_req.getOperation)
//      DataNode.read(boxset_req).foreach(node => {
//        node_present = true
//        val visibility: String = node.getMetadata.getOrDefault("visibility", "").asInstanceOf[String]
//        val versionKey: String = node.getMetadata.getOrDefault("versionKey", "").asInstanceOf[String]
//
//        if (StringUtils.equals(visibility, "parent"))
//          throw new ClientException(ContentConstants.ERR_USED_BOOK_ID, s"This book's visibility is parent  : ${node.getIdentifier}.")
//
//        book_cost += node.getMetadata.get("cost").asInstanceOf[Long]
//
//        val book = new util.HashMap[String, Object]()
//        book.putAll(Map("identifier" -> bookList.get(i), "versionKey" -> versionKey,"visibility" -> "parent").asJava)
//        val book_context = new util.HashMap[String, Object]()
//        book_context.putAll(Map("identifier"->bookList.get(i), "graph_id"->"domain", "schemaName"->"book", "version"->"1.0", "objectType"->"Book").asJava)
//
//        val book_req: Request = new Request(book_context, book, "updateNode", null)
//        book_req.getContext.put("identifier", bookList.get(i))
//        book_req.getContext.put("objectType", "Book")
//        book_req.getRequest.put("artifactUrl",null)
//
//
//        println("Book 1",book_req, book_req.getParams, book_req.getClass)
//        println("Book 2",book_req.getOperation)
//        DataNode.update(book_req).map(node_1 => {
//          ResponseHandler.OK.putAll(Map("identifier" -> node_1.getIdentifier.replace(".img", ""), "versionKey" -> node_1.getMetadata.get("versionKey")).asJava)
//        })
//
//        println(visibility.getClass, book_cost,book_cost.getClass, "+++++++++++++")
//        println(node.getMetadata.get("cost"),node.getMetadata.get("visibility"))
//        println(node.getMetadata, node.getExternalData, node.getIdentifier, node.getNode)
//      })
//      Thread.sleep(2000)
//      if (!node_present)
//        throw new ClientException(ContentConstants.ERR_NULL_BOOK_ID, s"Invalid Book ID  : ${bookList.get(i)}.")
//
//
//    }
//    Thread.sleep(7000) // wait for 7 seconds
//    request.getRequest.put("cost", book_cost * 0.80)
//    request.getRequest.put("bookCount", bookList.size())
//
//    if (bookList.size() < 3 )
//      throw new ClientException(ContentConstants.ERR_BOXSET_BOOK_COUNT, "Less than two books or bookCount doesn't match with bookList size")

    DataNode.create(request).map(node =>{
          val response = ResponseHandler.OK
          response.putAll(Map("identifier" -> node.getIdentifier, "versionKey" -> node.getMetadata.get("versionKey")).asJava)
    })
  }

}
