package org.sunbird.content.actors

import org.apache.commons.lang3.StringUtils
import org.apache.kafka.common.utils.Java
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
import scala.collection.{JavaConverters, mutable}

//import java.time.
import javax.inject.Inject
import collection.JavaConversions._
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class BoxSetActor @Inject()(implicit oec: OntologyEngineContext) extends BaseActor {
  implicit var bookCost: Long = 0
  implicit val ec: ExecutionContext = getContext().dispatcher

  override def onReceive(request: Request): Future[Response] = request.getOperation match {
    case "createBoxset" => create(request)
    case "readBoxset" => read(request, "boxset")
    case "addBook" => addBook(request)
    case "removeBook" => remove(request)
    case _ => ERROR(request.getOperation)
  }

  def first_fun(request: Request, book: String) = {

    val bookRead = new Request(request)
    bookRead.getContext.put("graph_id", "domain")
    bookRead.getContext.put("schemaName", "book")
    bookRead.getContext.put("objectType", "Book")
    bookRead.getContext.put("version", "1.0")

    bookRead.setObjectType("Book")

    val bookValue = new util.HashMap[String, Object]()
    bookValue.putAll(Map("identifier" -> book).asJava)
    bookRead.copyRequestValueObjects(bookValue)
    bookRead.setOperation("readBook")

    println("Book read 1", bookRead, bookRead.getClass)
    println("Book read 2", bookRead.getOperation)

    DataNode.read(bookRead).map(node => {
      bookCost += node.getMetadata.get("cost").asInstanceOf[Long]
      println(bookCost, "inside read")

    })

  }

  def check_n_create(request: Request)(implicit oec: OntologyEngineContext, ec: ExecutionContext): Future[Response] = {
    val bookList = request.getRequest.getOrDefault("bookList", new util.ArrayList[String]).asInstanceOf[util.List[String]]
    if (bookList.size() < 2)
      throw new ClientException(ContentConstants.ERR_BOXSET_BOOK_COUNT, "Less than two books or bookCount doesn't match with bookList size")

    bookList.map(book => {
      first_fun(request, book)

    })
    println(bookCost, "before update")
    request.getRequest.put("bookCount", bookList.size().asInstanceOf[Object])
    request.getRequest.put("cost", (bookCost * 0.80).asInstanceOf[Object])
    println(request, "req")
    //    Check before adding to boxset if all good comment below line
    throw new ClientException(ContentConstants.ERR_BOXSET_BOOK_COUNT, "Less than two books or bookCount doesn't match with bookList size")
    DataNode.create(request).map(node => {
      println(request, "Final +============")
      val response = ResponseHandler.OK
      response.putAll(Map("identifier" -> node.getIdentifier, "versionKey" -> node.getMetadata.get("versionKey")).asJava)
    })
  }

  def create(request: Request)(implicit oec: OntologyEngineContext, ec: ExecutionContext): Future[Response] = {
    RequestUtil.restrictProperties(request)
    bookCost = 0
    check_n_create(request)
  }

  def addBook(request: Request)(implicit oec: OntologyEngineContext, ec: ExecutionContext): Future[Response] = {
    RequestUtil.restrictProperties(request)
    println("11", request, request.get("fields"))
    println("22", request.getContext, request.getParams)

    DataNode.read(request).flatMap(node => {
      println(node, node.getMetadata, node.getExternalData, "yyyyyyyyy")
      var bookList: util.List[String] = JavaConverters.seqAsJavaListConverter(node.getMetadata.get("bookList").asInstanceOf[String].replace("[", "").replace("]", "").replace("\"", "").split(",").filter(field => StringUtils.isNotBlank(field) && !StringUtils.equalsIgnoreCase(field, "null"))).asJava
      val newBookList = request.getRequest.getOrDefault("bookList", new util.ArrayList[String]).asInstanceOf[util.List[String]]
      bookCost = 0
      val cost = node.getMetadata.get("cost").asInstanceOf[Number].longValue

      println(cost, "Cost", cost.getClass)
      println(newBookList, bookList, JavaConverters.seqAsJavaListConverter(node.getMetadata.get("bookList").asInstanceOf[String]), "zzzz")

      newBookList.map(book => {
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

        println("Book read 1", bookRead, bookRead.getClass)
        println("Book read 2", bookRead.getOperation)

        DataNode.read(bookRead).map(node1 => {
          bookCost += node1.getMetadata.get("cost").asInstanceOf[Long]
          println(bookCost, "inside readnode")
        })
      })

      if (bookList.exists {
        newBookList.contains
      })
        throw new ClientException(ContentConstants.EXISTS_BOOK_ID, s"One Book ID already exists")
      bookList = bookList ++ newBookList
      request.getRequest.put("bookList", bookList)

      bookCost = (bookCost * 0.80).asInstanceOf[Number].longValue() + cost
      request.getRequest.put("cost", bookCost.asInstanceOf[Object])
      request.getRequest.put("bookCount", (bookList.size()).asInstanceOf[Object])

      request.getRequest.remove("mode", "edit")
      request.getRequest.remove("fields", request.getRequest.get("fields"))

      println(bookList, newBookList, cost, bookCost, "RRRRRRRRR")
      println("444", request, request.getParams)

      println(request, "final+++++++++")
      //    Check before adding to boxset if all good comment below line
      throw new ClientException(ContentConstants.EXISTS_BOOK_ID, s"One Book ID already exists")
      DataNode.update(request).map(node2 => {
        val response = ResponseHandler.OK
        response.putAll(Map("identifier" -> node2.getIdentifier.replace(".img", ""), "versionKey" -> node2.getMetadata.get("versionKey")).asJava)

      })
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

          println("Book read 1", bookRead, bookRead.getClass)
          println("Book read 2", bookRead.getOperation)

          DataNode.read(bookRead).map(node1 => {
            metadata.put("bookData " + node1.getIdentifier, NodeUtil.serialize(node1, List(), node1.getObjectType.toLowerCase.replace("Image", ""), request.getContext.get("version").asInstanceOf[String]))

          })
        })
        ResponseHandler.OK.put(resName, metadata)
      }
    })
  }

  def remove(request: Request)(implicit oec: OntologyEngineContext, ec: ExecutionContext): Future[Response] = {
    RequestUtil.restrictProperties(request)
    val deleteBookList = request.getRequest.getOrDefault("bookList", new util.ArrayList[String]).asInstanceOf[util.List[String]]
    bookCost = 0
    DataNode.read(request).map(node => {
      println(node, node.getMetadata, node.getExternalData, "yyyyyyyyy")
      var cost = node.getMetadata.get("cost").asInstanceOf[Number].longValue
      var bookList: util.List[String] = JavaConverters.seqAsJavaListConverter(node.getMetadata.get("bookList").asInstanceOf[String].replace("[", "").replace("]", "").replace("\"", "").split(",").filter(field => StringUtils.isNotBlank(field) && !StringUtils.equalsIgnoreCase(field, "null"))).asJava
      val deleteBookList = request.getRequest.getOrDefault("bookList", new util.ArrayList[String]).asInstanceOf[util.List[String]]

      if (bookList.size() - deleteBookList.size() < 2)
        throw new ClientException(ContentConstants.ERR_MIN_BOOK, "Boxset requires min 2 books after deletion")
      bookList = bookList.filter(x => deleteBookList.contains(x))
      println(bookList, deleteBookList, "Books")
      deleteBookList.map(book => {
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

        println("Book read 1", bookRead, bookRead.getClass)
        println("Book read 2", bookRead.getOperation)

        DataNode.read(bookRead).map(node1 => {
          bookCost += node1.getMetadata.get("cost").asInstanceOf[Long]
        })
      }).map(_ => {
        cost = (cost - (bookCost * 0.80)).asInstanceOf[Number].longValue()
        request.getRequest.put("bookList", bookList)
        request.getRequest.put("cost", cost.asInstanceOf[Object])
        request.getRequest.put("bookCount", (bookList.size()).asInstanceOf[Object])

        request.getRequest.remove("mode", "edit")
        request.getRequest.remove("fields", request.getRequest.get("fields"))
      })

    })
    print(request, "final+++++++++")
    throw new ClientException(ContentConstants.EXISTS_BOOK_ID, s"One Book ID already exists")

    DataNode.update(request).map(node => {
      val response = ResponseHandler.OK
      response.putAll(Map("identifier" -> node.getIdentifier, "versionKey" -> node.getMetadata.get("versionKey")).asJava)
    })
  }


  // Temp code

  def getReadNode(book_read: Request)(implicit ec: ExecutionContext, oec: OntologyEngineContext): Future[Node] = {

    DataNode.read(book_read).map(node => {
      val visibility: String = node.getMetadata.getOrDefault("visibility", "").asInstanceOf[String]
      println(visibility, "VISIISIISISSI")

      //      if (StringUtils.equals(visibility, "parent"))
      //        throw new ClientException(ContentConstants.ERR_USED_BOOK_ID, s"This book's visibility is parent  : ${node.getIdentifier}.")

      node
    })
  }

  def check_all_constraints_boxset(request: Request) = {
    var book_cost: Long = 0
    val bookList = request.getRequest.getOrDefault("bookList", new util.ArrayList[String]).asInstanceOf[util.List[String]]

    for (i <- 0 to bookList.size() - 1) {
      var node_present = false

      val book_read = new Request(request)
      book_read.getContext.put("graph_id", "domain")
      book_read.getContext.put("schemaName", "book")
      book_read.getContext.put("objectType", "Book")
      book_read.getContext.put("version", "1.0")

      book_read.setObjectType("Book")

      val book_value = new util.HashMap[String, Object]()
      book_value.putAll(Map("identifier" -> bookList.get(i)).asJava)
      book_read.copyRequestValueObjects(book_value)
      book_read.setOperation("readBook")

      println("Book read 1", book_read, book_read.getClass)
      println("Book read 2", book_read.getOperation)

      getReadNode(book_read).map(node => {
        node_present = true
        val versionKey: String = node.getMetadata.getOrDefault("versionKey", "").asInstanceOf[String]

        book_cost += node.getMetadata.get("cost").asInstanceOf[Long]

        //        val book_update = new Request(request)
        //        book_update.getContext.put("graph_id","domain")
        //        book_update.getContext.put("schemaName","book")
        //        book_update.getContext.put("identifier",bookList.get(i))
        //        book_update.getContext.put("objectType","Book")
        //        book_update.getContext.put("version","1.0")
        ////        book_update.getContext.put("consumerId","X-Consumer-ID")
        //
        //        book_update.setObjectType("Book")
        //
        //        val book_update_value = new util.HashMap[String, Object]()
        //        book_update_value.putAll(Map("versionKey" -> versionKey,"identifier" -> bookList.get(i), "visibility" -> "parent").asJava)
        //        book_update.copyRequestValueObjects(book_update_value)

        //        val fields_modify: util.List[String] = JavaConverters.seqAsJavaListConverter("synopsis".split(",").filter(field => StringUtils.isNotBlank(field) && !StringUtils.equalsIgnoreCase(field, "null"))).asJava
        //        book_update.getRequest.put("fields", fields_modify)
        //        book_update.getRequest.put("mode", "edit")

        //        book_update.setOperation("updateBook")
        //        book_update.getContext.remove("channel", "{{channel_id}}")
        //        println("Book update 1",book_update, book_update.getClass)
        //        println("Book update 2",book_update.getOperation)


        //        DataNode.update(book_update).map(node_1 => {
        //          ResponseHandler.OK.putAll(Map("identifier" -> node_1.getIdentifier.replace(".img", ""), "versionKey" -> node_1.getMetadata.get("versionKey")).asJava)
        //        })
        //        println(visibility.getClass, book_cost,book_cost.getClass, "+++++++++++++")
        //        println(node.getMetadata.get("cost").getClass,node.getMetadata.get("visibility"))
        //        println(node.getMetadata, node.getExternalData, node.getIdentifier, node.getNode)

        //        book_update.getRequest.remove("mode","edit")
        //        book_update.getRequest.remove("fields", book_update.getRequest.get("fields"))

      })
      Thread.sleep(5000)
      if (!node_present)
        throw new ClientException(ContentConstants.ERR_NULL_BOOK_ID, s"Invalid Book ID  : ${bookList.get(i)}.")

    }
    request.getRequest.put("bookCount", bookList.size().asInstanceOf[Object])
    request.getRequest.put("cost", (book_cost * 0.80).asInstanceOf[Object])

    //    if (bookList.size() < 3 )
    //      throw new ClientException(ContentConstants.ERR_BOXSET_BOOK_COUNT, "Less than two books or bookCount doesn't match with bookList size")
  }

}