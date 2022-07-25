package org.sunbird.content.actors

import org.apache.commons.lang3.StringUtils
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
    var book_cost = 0

    val bookList = request.getRequest.getOrDefault("bookList", new util.ArrayList[String]).asInstanceOf[util.List[String]]
    val bookCount: Int= request.getRequest.getOrDefault("bookCount", java.lang.Integer.valueOf(0)).asInstanceOf[Int]
    val cost: Int= request.getRequest.getOrDefault("cost", java.lang.Integer.valueOf(0)).asInstanceOf[Int]

    if (bookList.size() < 2 || bookCount < 2 || bookCount != bookList.size())
      throw new ClientException(ContentConstants.ERR_BOXSET_BOOK_COUNT, "Less than two books or bookCount doesn't match with bookList size")

    bookList.foreach(x=> {
      var node_present = false
      val boxSet = new java.util.HashMap[String, Object]()
      boxSet.putAll(Map("identifier" -> x, "mode" -> "read").asJava)

      DataNode.read(new Request(request.getContext(), boxSet, "readNode", null)).map(node => {
        node_present = true
        val visibility: String = node.getMetadata.getOrDefault("visibility", "").asInstanceOf[String]

        if (StringUtils.equalsIgnoreCase(visibility, "Parent"))
          throw new ClientException(ContentConstants.ERR_USED_BOOK_ID, s"This book's visibility is parent  : ${node.getIdentifier}.")

        book_cost = book_cost + node.getMetadata.getOrDefault("cost", java.lang.Integer.valueOf(0)).asInstanceOf[Int]
        println(node.getMetadata, node.getExternalData, node.getIdentifier, node.getNode)
      })
      if (!node_present)
        throw new ClientException(ContentConstants.ERR_NULL_BOOK_ID, s"This book id : ${x} is not valid.")

    })
    if (cost > book_cost * 0.80)
      throw new ClientException(ContentConstants.ERR_BOXSET_COST, "Boxset cost is greater than 80% of total book cost")
  }
  def create(request: Request): Future[Response] = {
    RequestUtil.restrictProperties(request)
//    val bookList = request.getRequest.getOrDefault("bookList", new util.ArrayList[java.util.Map[String, AnyRef]]).asInstanceOf[util.List[java.util.Map[String, AnyRef]]]

//    println("-----------pppppppppppp---------------")
//    println(request, request.getClass)
//    println(bookCount,bookCount.getClass)
//    println(bookList, bookList.getClass)
//    println("-------------PPPPPPPPPP-------------")

//    check_all_constraints_boxset(request)
    var book_cost: Long = 0

    val bookList = request.getRequest.getOrDefault("bookList", new util.ArrayList[String]).asInstanceOf[util.List[String]]
    val bookCount: Int= request.getRequest.getOrDefault("bookCount", java.lang.Integer.valueOf(0)).asInstanceOf[Int]
    val cost: Int= request.getRequest.getOrDefault("cost", java.lang.Integer.valueOf(0)).asInstanceOf[Int]

    for (i <- 0 to bookList.size()-1) {
      val boxSet = new java.util.HashMap[String, Object]()
      boxSet.putAll(Map("identifier" -> bookList.get(i), "mode" -> "read").asJava)

      DataNode.read(new Request(request.getContext(), boxSet, "readNode", null)).foreach(node => {
        val visibility: String = node.getMetadata.getOrDefault("visibility", "").asInstanceOf[String]

        if (StringUtils.equals(visibility, "Parent"))
          throw new ClientException(ContentConstants.ERR_USED_BOOK_ID, s"This book's visibility is parent  : ${node.getIdentifier}.")

        book_cost += node.getMetadata.get("cost").asInstanceOf[Long]

        println(visibility.getClass, book_cost,book_cost.getClass, "+++++++++++++")
        println(node.getMetadata.get("cost"),node.getMetadata.get("visibility"))
        println(node.getMetadata, node.getExternalData, node.getIdentifier, node.getNode)
      })

    }
    Thread.sleep(10000) // wait for 10 seconds
    println(cost, book_cost)
    if (cost.asInstanceOf[Long] > book_cost * 0.80)
      throw new ClientException(ContentConstants.ERR_BOXSET_COST, "Boxset cost is greater than 80% of total book cost")

    if (bookList.size() < 2 || bookCount < 2 || bookCount != bookList.size())
      throw new ClientException(ContentConstants.ERR_BOXSET_BOOK_COUNT, "Less than two books or bookCount doesn't match with bookList size")


    DataNode.create(request).map(node =>{
          val response = ResponseHandler.OK
          response.putAll(Map("identifier" -> node.getIdentifier, "versionKey" -> node.getMetadata.get("versionKey")).asJava)
    })
  }

}
