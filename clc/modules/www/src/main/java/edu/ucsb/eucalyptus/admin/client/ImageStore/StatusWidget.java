package edu.ucsb.eucalyptus.admin.client.ImageStore;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.Grid;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ClickEvent;


public class StatusWidget extends Composite {

    private DisclosurePanel disclosurePanel = new DisclosurePanel();
    private Label headerLabel = new Label();
    private Grid contentGrid = new Grid(1, 3); 

    private static class ImageData {
        public int rowIndex = -1;
        ImageInfo info = null;
        ImageState state = null;
    }

    private static class StatusCounts {
        public int downloading = 0;
        public int installing = 0;
        public int errorMessages = 0;

        public boolean allZeros() {
            return downloading == 0 && installing == 0 && errorMessages == 0;
        }
    }

    private Map<String,ImageData> imageDataMap = new HashMap<String,ImageData>();

    public void clear() {
        imageDataMap.clear();
        contentGrid.resize(1, 3);
    }

    public StatusWidget() {
        disclosurePanel.setHeader(headerLabel);
        disclosurePanel.setContent(new FrameWidget(contentGrid));

        contentGrid.setCellSpacing(0);
        contentGrid.setCellPadding(0);

        Label titleLabel = new Label("Image title");
        Label statusLabel = new Label("Status");

        titleLabel.addStyleName("istore-table-header");
        statusLabel.addStyleName("istore-table-header");

        contentGrid.setWidget(0, 0, titleLabel);
        contentGrid.setWidget(0, 1, statusLabel);

        contentGrid.getRowFormatter().addStyleName(0, "istore-odd");

        initWidget(disclosurePanel);

        setStyleName("istore-status-widget");

        headerLabel.setStyleName("istore-header-label");
        contentGrid.setStyleName("istore-table");
    }

    public void putImageInfo(ImageInfo imageInfo) {
        ImageData imageData = imageDataMap.get(imageInfo.getUri());
        if (imageData != null) {
            imageData.info = imageInfo;
            updateImageDisplay(imageData);
        } else {
            imageData = new ImageData();
            imageData.info = imageInfo;
            imageDataMap.put(imageInfo.getUri(), imageData);
        }
    }

    public void putImageState(ImageState imageState) {
        ImageData imageData = imageDataMap.get(imageState.getImageUri());
        if (imageData != null) {
            imageData.state = imageState;
            updateImageDisplay(imageData);
        } else {
            imageData = new ImageData();
            imageData.state = imageState;
            imageDataMap.put(imageState.getImageUri(), imageData);
        }
    }

    private void updateImageDisplay(ImageData imageData) {
        // Only insert or update this image's status if we have the needed
        // data.  Also, if we haven't yet displayed this image, only display
        // if its current status is transient or if it's an error.
        if (imageData.info != null && imageData.state != null &&
            (imageData.rowIndex != -1 ||
             imageData.state.getStatus().isTransient() ||
             imageData.state.getErrorMessage() != null)) {
            updateGrid(imageData);
            if (isAttached()) {
                updateDisplay();
            }
        }
    }

    private void updateDisplay() {
        StatusCounts statusCounts = getStatusCounts();
        if (statusCounts.allZeros()) {
            // Do not use setVisible() here to prevent scrolling the page
            // when the status changes.
            headerLabel.setText("");
            disclosurePanel.setOpen(false);
        } else {
            updateHeader(statusCounts);
        }
    }

    protected void onAttach() {
        updateDisplay();
        super.onAttach();
    }

    private StatusCounts getStatusCounts() {
        StatusCounts statusCounts = new StatusCounts();

        for (ImageData imageData : imageDataMap.values()) {
            if (imageData.info != null && imageData.state != null) {
                switch (imageData.state.getStatus()) {
                    case INSTALLING:
                        statusCounts.installing++;
                        break;
                    case DOWNLOADING:
                        statusCounts.downloading++;
                        break;
                }

                if (imageData.state.getErrorMessage() != null) {
                    statusCounts.errorMessages++;
                }
            }
        }

        return statusCounts;
    }

    private void updateGrid(ImageData imageData) {
        final ImageInfo imageInfo = imageData.info;
        final ImageState imageState = imageData.state;

        assert imageInfo != null && imageState != null;

        int rowIndex = imageData.rowIndex;

        if (rowIndex == -1) {
            // Image isn't yet being displayed.  Add a new row for it.
            imageData.rowIndex = rowIndex = contentGrid.getRowCount();
            contentGrid.resizeRows(rowIndex + 1);
            contentGrid.getRowFormatter().addStyleName(rowIndex,
                    rowIndex % 2 == 0 ?
                    "istore-odd" : "istore-even");
        }

        ImageState.Status status = imageState.getStatus();
        String statusRepr = status.toString().toLowerCase();
        if (status.isTransient()) {
            statusRepr += "...";
        }
        Label titleLabel = new Label(imageInfo.getTitle());
        Label statusLabel = new Label(statusRepr);

        titleLabel.addStyleName("istore-image-title");
        statusLabel.addStyleName("istore-image-status");

        contentGrid.setWidget(rowIndex, 0, titleLabel);
        contentGrid.setWidget(rowIndex, 1, statusLabel);

        if (imageState.getErrorMessage() == null) {
            contentGrid.clearCell(rowIndex, 2);
        } else {
            final Anchor errorAnchor = new Anchor("(show error)");
            ImageErrorDialog errorDialog = new ImageErrorDialog(imageInfo, imageState);
            errorDialog.connectClickHandler(errorAnchor);
            errorDialog.addClearErrorHandler(new ClearErrorHandler() {
                public void onClearError(ClearErrorEvent event) {
                    // Forward the event to subscribers of this widget.
                    StatusWidget.this.fireEvent(event);
                }
            });
            errorAnchor.setStyleName("istore-show-error-anchor");
            contentGrid.setWidget(rowIndex, 2, errorAnchor);
        }
    }

    private void updateHeader(StatusCounts statusCounts) {

        StringBuilder builder = new StringBuilder();

        if (statusCounts.downloading != 0) {
            builder.append(statusCounts.downloading);
            if (statusCounts.downloading == 1) {
                builder.append(" image downloading");
            } else {
                builder.append(" images downloading");
            }
        }

        if (statusCounts.installing != 0) {
            if (builder.length() != 0) {
                builder.append(", ");
            }
            builder.append(statusCounts.installing);
            if (statusCounts.installing == 1) {
                builder.append(" image installing");
            } else {
                builder.append(" images installing");
            }
        }

        if (statusCounts.errorMessages != 0) {
            if (builder.length() != 0) {
                builder.append(", ");
            }
            builder.append(statusCounts.errorMessages);
            if (statusCounts.errorMessages == 1) {
                builder.append(" error message");
            } else {
                builder.append(" error messages");
            }
        }

        if (builder.length() == 0) {
            builder.append("No requested changes in progress");
        }

        headerLabel.setText(builder.toString());
    }

    public void addClearErrorHandler(ClearErrorHandler<ImageState> handler) {
        addHandler(handler, ClearErrorEvent.getType());
    }

}
