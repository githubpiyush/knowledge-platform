package org.sunbird.content.actors

import org.sunbird.actor.core.BaseActor
import org.sunbird.common.dto.{Request, Response, ResponseHandler}
import org.sunbird.graph.OntologyEngineContext
import org.sunbird.graph.nodes.DataNode
import org.sunbird.util.RequestUtil

import javax.inject.Inject
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

class BoxSetActor @Inject()(implicit oec: OntologyEngineContext) extends BaseActor {

  implicit val ec: ExecutionContext = getContext().dispatcher

  override def onReceive(request: Request): Future[Response] = request.getOperation match {
    case "createBoxset" => create(request)
    //case "readBoxset" =>
    //case "updateBoxset" =>
    case _ => ERROR(request.getOperation)
  }

  def create(request: Request): Future[Response] = {
    RequestUtil.restrictProperties(request)
    DataNode.create(request).map(node =>{
          val response = ResponseHandler.OK
          response.putAll(Map("identifier" -> node.getIdentifier, "versionKey" -> node.getMetadata.get("versionKey")).asJava)
    })
  }

}
