package controllers.v4

import akka.actor.{ActorRef, ActorSystem}
import controllers.BaseController
import com.google.inject.Singleton
import org.apache.commons.lang3.StringUtils
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import utils.{ActorNames, ApiId, Constants}

import java.util
import javax.inject.{Inject, Named}
import scala.collection.JavaConverters
import scala.collection.JavaConverters.mapAsJavaMapConverter
import scala.concurrent.ExecutionContext

@Singleton
class BoxSetController @Inject()(@Named(ActorNames.BOXSET_ACTOR) boxsetActor: ActorRef, cc: ControllerComponents, actorSystem: ActorSystem)
                                (implicit exec: ExecutionContext) extends BaseController(cc) {

  val objectType = "BoxSet"
  val schemaName: String = "boxset"
  val version = "1.0"

  def create() = Action.async { implicit request =>
    val headers = commonHeaders()
    val body = requestBody()
    val boxset = body.getOrDefault(schemaName, new java.util.HashMap()).asInstanceOf[java.util.Map[String, AnyRef]];
    boxset.putAll(headers)
    println(headers, body, boxset)

    val boxsetRequest = getRequest(boxset, headers, "createBoxset")
    setRequestContext(boxsetRequest, version, objectType, schemaName)
    println(boxsetRequest)
    getResult(ApiId.CREATE_BOXSET, boxsetActor, boxsetRequest)
  }

  def read(identifier: String, mode: Option[String], fields: Option[String]) = Action.async { implicit request =>
    val headers = commonHeaders()
    val boxset = new java.util.HashMap[String, Object]()
    boxset.putAll(headers)
    boxset.putAll(Map("identifier" -> identifier, "mode" -> mode.getOrElse(""), "fields" -> fields.getOrElse("")).asJava)
    val boxsetRequest = getRequest(boxset, headers, "readBoxset")
    setRequestContext(boxsetRequest, version, objectType, schemaName)
//    readRequest.getContext.put(Constants.RESPONSE_SCHEMA_NAME, schemaName);
    getResult(ApiId.READ_BOXSET, boxsetActor, boxsetRequest)
  }

  def addBook(identifier: String, fields: Option[String]) = Action.async { implicit request =>
    val headers = commonHeaders()
    val body = requestBody()
    val boxset = body.getOrDefault(schemaName, new java.util.HashMap()).asInstanceOf[java.util.Map[String, AnyRef]];
    boxset.putAll(headers)
    boxset.putAll(Map("fields" -> fields.getOrElse(""),"mode"->"edit").asJava)

    val boxsetRequest = getRequest(boxset, headers, "addBook")
    setRequestContext(boxsetRequest, version, objectType, schemaName)
    boxsetRequest.getContext.put("identifier", identifier);
    boxsetRequest.getRequest.put("identifier", boxsetRequest.getContext.get("identifier"))
    val fields_modify: util.List[String] = JavaConverters.seqAsJavaListConverter(boxsetRequest.get("fields").asInstanceOf[String].split(",").filter(field => StringUtils.isNotBlank(field) && !StringUtils.equalsIgnoreCase(field, "null"))).asJava
    boxsetRequest.getRequest.put("fields", fields_modify)

    getResult(ApiId.ADD_TO_BOXSET, boxsetActor, boxsetRequest)

  }

  def removeBook(identifier: String, fields: Option[String]) = Action.async { implicit request =>
    val headers = commonHeaders()
    val body = requestBody()
    val boxset = body.getOrDefault(schemaName, new java.util.HashMap()).asInstanceOf[java.util.Map[String, AnyRef]];
    boxset.putAll(headers)
    boxset.putAll(Map("fields" -> fields.getOrElse(""),"mode"->"edit").asJava)

    val boxsetRequest = getRequest(boxset, headers, "removeBook")
    setRequestContext(boxsetRequest, version, objectType, schemaName)
    boxsetRequest.getContext.put("identifier", identifier);
    boxsetRequest.getRequest.put("identifier", boxsetRequest.getContext.get("identifier"))
    val fields_modify: util.List[String] = JavaConverters.seqAsJavaListConverter(boxsetRequest.get("fields").asInstanceOf[String].split(",").filter(field => StringUtils.isNotBlank(field) && !StringUtils.equalsIgnoreCase(field, "null"))).asJava
    boxsetRequest.getRequest.put("fields", fields_modify)

    getResult(ApiId.DELETE_BOOK_BOXSET, boxsetActor, boxsetRequest)
  }


}
