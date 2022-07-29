package controllers.v4

import akka.actor.{ActorRef, ActorSystem}
import controllers.BaseController
import com.google.inject.Singleton
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import utils.{ActorNames, ApiId, Constants}

import javax.inject.{Inject, Named}
import scala.collection.JavaConverters.mapAsJavaMapConverter
import scala.concurrent.ExecutionContext

@Singleton
class BookController @Inject()(@Named(ActorNames.BOOK_ACTOR) bookActor: ActorRef, cc: ControllerComponents, actorSystem: ActorSystem)
                                (implicit exec: ExecutionContext) extends BaseController(cc) {

  val objectType = "Book"
  val schemaName: String = "book"
  val version = "1.0"

  def create() = Action.async { implicit request =>
    val headers = commonHeaders()
    val body = requestBody()
    val book = body.getOrDefault(schemaName, new java.util.HashMap()).asInstanceOf[java.util.Map[String, AnyRef]];
    book.putAll(headers)
    println(headers, body, book)

    val bookRequest = getRequest(book, headers, "createBook")
    setRequestContext(bookRequest, version, objectType, schemaName)
    println(bookRequest)
    getResult(ApiId.CREATE_BOOK, bookActor, bookRequest)

  }

  def read(identifier: String, mode: Option[String], fields: Option[String]) = Action.async { implicit request =>
    val headers = commonHeaders()
    val book = new java.util.HashMap[String, Object]()
    book.putAll(headers)
    book.putAll(Map("identifier" -> identifier, "mode" -> mode.getOrElse("read"), "fields" -> fields.getOrElse("")).asJava)
    val bookRequest = getRequest(book, headers, "readBook")
    setRequestContext(bookRequest, version, objectType, schemaName)
    //    readRequest.getContext.put(Constants.RESPONSE_SCHEMA_NAME, schemaName);
    getResult(ApiId.READ_BOOK, bookActor, bookRequest)
  }

  def update(identifier: String) = Action.async { implicit request =>
    val headers = commonHeaders()
    val body = requestBody()
    val book = body.getOrDefault(schemaName, new java.util.HashMap()).asInstanceOf[java.util.Map[String, Object]];
    book.putAll(headers)
    val bookRequest = getRequest(book, headers, "updateBook")
    setRequestContext(bookRequest, version, objectType, schemaName)
    bookRequest.getContext.put("identifier", identifier);
    getResult(ApiId.UPDATE_BOXSET, bookActor, bookRequest)

  }

}
