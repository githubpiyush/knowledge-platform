package org.sunbird.content.actors

import org.apache.commons.lang3.StringUtils
import org.sunbird.actor.core.BaseActor
import org.sunbird.common.dto.{Request, Response, ResponseHandler}
import org.sunbird.common.exception.ClientException
import org.sunbird.content.util.ContentConstants
import org.sunbird.graph.OntologyEngineContext
import org.sunbird.graph.dac.model.Node
import org.sunbird.graph.nodes.DataNode
import org.sunbird.graph.utils.NodeUtil
import org.sunbird.util.RequestUtil

import java.util
import java.util.concurrent.CompletionException
import scala.collection.JavaConverters

//import java.time.
import javax.inject.Inject
import collection.JavaConversions._
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

class BoxSetActor @Inject()(implicit oec: OntologyEngineContext) extends BaseActor {
  implicit val ec: ExecutionContext = getContext().dispatcher

  override def onReceive(request: Request): Future[Response] = request.getOperation match {
    case "createBoxset" => create(request)
    case "readBoxset" => read(request, "boxset")
    case "addBook" => addBook(request)
    case "removeBook" => removeBook(request)
    case _ => ERROR(request.getOperation)
  }

  def getTotalCost(request: Request, BookList: util.List[String],size: Int, total:Long = 0, updatedVisibility:String = "parent"):Future[Long]={
    if(size == 0) Future(total)
    else{
      val book = BookList.apply(size-1)
      val bookRead = new Request(request)
      bookRead.getContext.put("graph_id", "domain")
      bookRead.getContext.put("schemaName", "book")
      bookRead.getContext.put("identifier", book)
      bookRead.getContext.put("objectType", "Book")
      bookRead.getContext.put("version", "1.0")

      bookRead.setObjectType("Book")

      val bookValue = new util.HashMap[String, Object]()
      bookValue.putAll(Map("identifier" -> book).asJava)
      bookRead.copyRequestValueObjects(bookValue)
      bookRead.setOperation("readBook")

//      println("Book read 1", bookRead, bookRead.getClass)
//      println("Book read 2", bookRead.getOperation)

      DataNode.read(bookRead).map(node1 => {
//        println(node1.getMetadata)
        val cost = node1.getMetadata.get("cost").asInstanceOf[Number].longValue()
        val visibility: String = node1.getMetadata.getOrDefault("visibility", "").asInstanceOf[String]
//        println(visibility, "VISIISIISISSI")

        if (StringUtils.equals(visibility, updatedVisibility))
          throw new ClientException(ContentConstants.ERR_USED_BOOK_ID, s"This book's visibility is ${updatedVisibility}  : ${node1.getIdentifier}.")

        val book_update = new Request(bookRead)
        val versionKey: String = node1.getMetadata.getOrDefault("versionKey", "").asInstanceOf[String]

        val book_update_value = new util.HashMap[String, Object]()
        book_update_value.putAll(Map("versionKey" -> versionKey,"identifier" -> book, "visibility" -> updatedVisibility).asJava)
        book_update.copyRequestValueObjects(book_update_value)

        book_update.setOperation("updateBook")
        book_update.setObjectType("Book")
        book_update.getContext.remove("channel", "{{channel_id}}")
//        println("Book update 1",book_update, book_update.getClass)
//        println("Book update 2",book_update.getOperation)


        DataNode.update(book_update).map(node2 => {
          Future(node2)
        })

        getTotalCost(request,BookList, size-1, total+cost)
      }).flatMap(f=>f)
    }
  }

  def check_n_create(request: Request)(implicit oec: OntologyEngineContext, ec: ExecutionContext): Future[Response] = {
    val bookList = request.getRequest.getOrDefault("bookList", new util.ArrayList[String]).asInstanceOf[util.List[String]]
    if (bookList.size() < 2)
      throw new ClientException(ContentConstants.ERR_BOXSET_BOOK_COUNT, "Less than two books or bookCount doesn't match with bookList size")

    getTotalCost(request,bookList,bookList.size()).flatMap(totalCost=> {
      request.getRequest.put("bookCount", bookList.size().asInstanceOf[Object])
      request.getRequest.put("cost", (totalCost * 0.80).asInstanceOf[Object])

      DataNode.create(request).map(node => {
        println(request, "Final +============")
        val response = ResponseHandler.OK
        response.putAll(Map("identifier" -> node.getIdentifier, "versionKey" -> node.getMetadata.get("versionKey")).asJava)
      })
    })
  }

  def create(request: Request)(implicit oec: OntologyEngineContext, ec: ExecutionContext): Future[Response] = {
    RequestUtil.restrictProperties(request)
    check_n_create(request)
  }


  def addBook(request: Request)(implicit oec: OntologyEngineContext, ec: ExecutionContext): Future[Response] = {
    RequestUtil.restrictProperties(request)

    DataNode.read(request).flatMap(node => {
//      println(node.getMetadata, node.getIdentifier)
      var bookList: util.List[String] = JavaConverters.seqAsJavaListConverter(node.getMetadata.get("bookList").asInstanceOf[String].replace("[", "").replace("]", "").replace("\"", "").split(",").filter(field => StringUtils.isNotBlank(field) && !StringUtils.equalsIgnoreCase(field, "null"))).asJava
      val newBookList = request.getRequest.getOrDefault("bookList", new util.ArrayList[String]).asInstanceOf[util.List[String]]
      var bookCost: Long = 0
      val cost = node.getMetadata.get("cost").asInstanceOf[Number].longValue

//      println(cost, "Cost", cost.getClass)
//      println(newBookList, bookList, JavaConverters.seqAsJavaListConverter(node.getMetadata.get("bookList").asInstanceOf[String]), "zzzz")

      getTotalCost(request,newBookList,newBookList.size()).map(totalCost=> {
        if (bookList.exists {newBookList.contains})
          throw new ClientException(ContentConstants.EXISTS_BOOK_ID, s"One Book ID already exists")
        bookList = bookList ++ newBookList
        request.getRequest.put("bookList", bookList)

        bookCost = (totalCost * 0.80).asInstanceOf[Number].longValue() + cost
        request.getRequest.put("cost", bookCost.asInstanceOf[Object])
        request.getRequest.put("bookCount", (bookList.size()).asInstanceOf[Object])

        request.getRequest.remove("mode", "edit")
        request.getRequest.remove("fields", request.getRequest.get("fields"))

//        println(bookList, newBookList, cost, bookCost, "RRRRRRRRR")
//        println("444", request, request.getParams)

        println(request, "final+++++++++")
        DataNode.update(request).map(node2 => {
//          println(node2,"inside update&&&&&&&")
          val response = ResponseHandler.OK
          response.putAll(Map("identifier" -> node2.getIdentifier.replace(".img", ""), "versionKey" -> node2.getMetadata.get("versionKey")).asJava)
          response
        })
      }).flatMap(f=>f) recoverWith { case e: CompletionException => throw e.getCause }
    })
}

  def read(request: Request, resName: String)(implicit oec: OntologyEngineContext, ec: ExecutionContext): Future[Response] = {
    RequestUtil.restrictProperties(request)
    val fields: util.List[String] = JavaConverters.seqAsJavaListConverter(request.get("fields").asInstanceOf[String].split(",").filter(field => StringUtils.isNotBlank(field) && !StringUtils.equalsIgnoreCase(field, "null"))).asJava
    request.getRequest.put("fields", fields)
    DataNode.read(request).map(node => {
      val metadata: util.Map[String, AnyRef] = NodeUtil.serialize(node, fields, node.getObjectType.toLowerCase.replace("Image", ""), request.getContext.get("version").asInstanceOf[String])
      metadata.put("identifier", node.getIdentifier.replace(".img", ""))

      if (StringUtils.equalsIgnoreCase(request.get("mode").asInstanceOf[String], "single")) {
        ResponseHandler.OK.put(resName, metadata)
      }
      else {

        val bookList: util.List[String] = JavaConverters.seqAsJavaListConverter(node.getMetadata.get("bookList").asInstanceOf[String].replace("[", "").replace("]", "").replace("\"", "").split(",").filter(field => StringUtils.isNotBlank(field) && !StringUtils.equalsIgnoreCase(field, "null"))).asJava

        bookList.map(book => {
          val bookRead = new Request(request)
          bookRead.getContext.put("graph_id", "domain")
          bookRead.getContext.put("schemaName", "book")
          bookRead.getContext.put("identifier", book)
          bookRead.getContext.put("objectType", "Book")
          bookRead.getContext.put("version", "1.0")

          bookRead.setObjectType("Book")

          val bookValue = new util.HashMap[String, Object]()
          bookValue.putAll(Map("identifier" -> book).asJava)
          bookRead.copyRequestValueObjects(bookValue)
          bookRead.setOperation("readBook")

//          println("Book read 1", bookRead, bookRead.getClass)
//          println("Book read 2", bookRead.getOperation)

          DataNode.read(bookRead).map(node1 => {
            metadata.put("bookData " + node1.getIdentifier, NodeUtil.serialize(node1, List(), node1.getObjectType.toLowerCase.replace("Image", ""), request.getContext.get("version").asInstanceOf[String]))

          })
        })
        ResponseHandler.OK.put(resName, metadata)
      }
    })
  }

  def removeBook(request: Request)(implicit oec: OntologyEngineContext, ec: ExecutionContext): Future[Response] = {
    RequestUtil.restrictProperties(request)
    val deleteBookList = request.getRequest.getOrDefault("bookList", new util.ArrayList[String]).asInstanceOf[util.List[String]]

    DataNode.read(request).flatMap(node => {
      var cost = node.getMetadata.get("cost").asInstanceOf[Number].longValue
      var bookList: util.List[String] = JavaConverters.seqAsJavaListConverter(node.getMetadata.get("bookList").asInstanceOf[String].replace("[", "").replace("]", "").replace("\"", "").split(",").filter(field => StringUtils.isNotBlank(field) && !StringUtils.equalsIgnoreCase(field, "null"))).asJava
      val deleteBookList = request.getRequest.getOrDefault("bookList", new util.ArrayList[String]).asInstanceOf[util.List[String]]

      if (bookList.size() - deleteBookList.size() < 2)
        throw new ClientException(ContentConstants.ERR_MIN_BOOK, "Boxset requires min 2 books after deletion")
      bookList = bookList diff deleteBookList
      getTotalCost(request, deleteBookList, deleteBookList.size(), updatedVisibility = "default").map(totalCost => {

        cost = (cost - (totalCost * 0.80)).asInstanceOf[Number].longValue()
        request.getRequest.put("bookList", bookList)
        request.getRequest.put("cost", cost.asInstanceOf[Object])
        request.getRequest.put("bookCount", (bookList.size()).asInstanceOf[Object])

        request.getRequest.remove("mode", "edit")
        request.getRequest.remove("fields", request.getRequest.get("fields"))

        print(request, "final+++++++++")

        DataNode.update(request).map(node => {
          val response = ResponseHandler.OK
          response.putAll(Map("identifier" -> node.getIdentifier, "versionKey" -> node.getMetadata.get("versionKey")).asJava)
        })

      })

    }).flatMap(f=>f) recoverWith { case e: CompletionException => throw e.getCause }
  }

}