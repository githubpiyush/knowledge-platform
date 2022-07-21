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
class BoxSetController @Inject()(@Named(ActorNames.BOXSET_ACTOR) boxsetActor: ActorRef, cc: ControllerComponents, actorSystem: ActorSystem)
                                (implicit exec: ExecutionContext) extends BaseController(cc) {

  val objectType = "BoxSet"
  val schemaName: String = "boxset"
  val version = "1.0"

  def create() = Action.async { implicit request =>
    val headers = commonHeaders()
    val body = requestBody()
    val boxset = body.getOrDefault(schemaName, new java.util.HashMap()).asInstanceOf[java.util.Map[String, Object]];
    boxset.putAll(headers)

    val boxsetRequest = getRequest(boxset, headers, "createBoxset")
    setRequestContext(boxsetRequest, version, objectType, schemaName)
    getResult(ApiId.CREATE_BOXSET, boxsetActor, boxsetRequest)
  }

  def read(identifier: String, mode: Option[String], fields: Option[String]) = Action.async { implicit request =>
    val headers = commonHeaders()
    val boxset = new java.util.HashMap[String, Object]()
    boxset.putAll(headers)
    boxset.putAll(Map("identifier" -> identifier, "mode" -> mode.getOrElse("read"), "fields" -> fields.getOrElse("")).asJava)
    val boxsetRequest = getRequest(boxset, headers, "readBoxset")
    setRequestContext(boxsetRequest, version, objectType, schemaName)
//    readRequest.getContext.put(Constants.RESPONSE_SCHEMA_NAME, schemaName);
    getResult(ApiId.READ_BOXSET, boxsetActor, boxsetRequest)
  }

  def update(identifier: String) = Action.async { implicit request =>
    val headers = commonHeaders()
    val body = requestBody()
    val boxset = body.getOrDefault(schemaName, new java.util.HashMap()).asInstanceOf[java.util.Map[String, Object]];
    boxset.putAll(headers)
    val boxsetRequest = getRequest(boxset, headers, "updateBoxset")
    setRequestContext(boxsetRequest, version, objectType, schemaName)
    boxsetRequest.getContext.put("identifier", identifier);
    getResult(ApiId.UPDATE_BOXSET, boxsetActor, boxsetRequest)

  }

}
