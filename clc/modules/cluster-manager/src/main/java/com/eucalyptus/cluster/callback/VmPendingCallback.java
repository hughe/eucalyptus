package com.eucalyptus.cluster.callback;

import java.util.NoSuchElementException;
import java.util.concurrent.CancellationException;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.cluster.VmInstance.Reason;
import com.eucalyptus.cluster.VmInstances;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.util.async.FailedRequestException;
import com.eucalyptus.vm.VmState;
import com.google.common.base.Predicate;
import edu.ucsb.eucalyptus.cloud.VmDescribeResponseType;
import edu.ucsb.eucalyptus.cloud.VmDescribeType;
import edu.ucsb.eucalyptus.cloud.VmInfo;
import edu.ucsb.eucalyptus.msgs.AttachedVolume;

public class VmPendingCallback extends StateUpdateMessageCallback<Cluster, VmDescribeType, VmDescribeResponseType> {
  private static Logger LOG = Logger.getLogger( VmPendingCallback.class );
  
  public VmPendingCallback( Cluster cluster ) {
    super( cluster );
    super.setRequest( new VmDescribeType( ) {
      {
        regarding( );
        for ( VmInstance vm : VmInstances.listValues( ) ) {
          if ( vm.getPartition( ).equals( VmPendingCallback.this.getSubject( ).getConfiguration( ).getPartition( ) ) ) {
            if ( VmState.PENDING.equals( vm.getState( ) )
                        || vm.getState( ).ordinal( ) > VmState.RUNNING.ordinal( ) ) {
              this.getInstancesSet( ).add( vm.getInstanceId( ) );
            } else if ( vm.eachVolumeAttachment( new Predicate<AttachedVolume>( ) {
              @Override
              public boolean apply( AttachedVolume arg0 ) {
                return !arg0.getStatus( ).endsWith( "ing" );
              }
            } ) ) {

            }
          }
        }
      }
    } );
    if ( this.getRequest( ).getInstancesSet( ).isEmpty( ) ) {
      throw new CancellationException( );
    }
  }
  
  @Override
  public void fire( VmDescribeResponseType reply ) {
    for ( final VmInfo runVm : reply.getVms( ) ) {
      runVm.setPlacement( this.getSubject( ).getConfiguration( ).getName( ) );
      VmState state = VmState.Mapper.get( runVm.getStateName( ) );
      EntityTransaction db = Entities.get( VmInstance.class );
      try {
        final VmInstance vm = VmInstances.lookup( runVm.getInstanceId( ) );
        vm.setServiceTag( runVm.getServiceTag( ) );
        if ( VmState.SHUTTING_DOWN.equals( vm.getRuntimeState( ) ) && vm.getSplitTime( ) > VmInstances.SHUT_DOWN_TIME ) {
          vm.setState( VmState.TERMINATED, Reason.EXPIRED );
        } else if ( VmState.SHUTTING_DOWN.equals( vm.getRuntimeState( ) ) && VmState.SHUTTING_DOWN.equals( state ) ) {
          vm.setState( VmState.TERMINATED, Reason.APPEND, "DONE" );
        } else if ( ( VmState.PENDING.equals( state ) || VmState.RUNNING.equals( state ) )
                    && ( VmState.PENDING.equals( vm.getRuntimeState( ) ) || VmState.RUNNING.equals( vm.getRuntimeState( ) ) ) ) {
          if ( !VmInstance.DEFAULT_IP.equals( runVm.getNetParams( ).getIpAddress( ) ) ) {
            vm.updateAddresses( runVm.getNetParams( ).getIpAddress( ), runVm.getNetParams( ).getIgnoredPublicIp( ) );
          }
          vm.setState( VmState.Mapper.get( runVm.getStateName( ) ), Reason.APPEND, "UPDATE" );
          vm.updateVolumeAttachments( runVm.getVolumes( ) );
        }
        db.commit( );
      } catch ( Exception ex ) {
        LOG.debug( "Ignoring update for uncached vm: " + runVm.getInstanceId( ) );
      }
    }
  }
  
  /**
   * @see com.eucalyptus.cluster.callback.StateUpdateMessageCallback#fireException(com.eucalyptus.util.async.FailedRequestException)
   * @param t
   */
  @Override
  public void fireException( FailedRequestException t ) {
    LOG.debug( "Request to " + this.getSubject( ).getName( ) + " failed: " + t.getMessage( ) );
  }
  
}
