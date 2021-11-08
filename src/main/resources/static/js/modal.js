class Modal extends React.Component {
    render() {
        if (!this.props.show) {
            return null;
        }

        return (
            <div className="modal">
                <div className="modal-content">
                    {this.props.children}
                </div>
            </div>
        );
    }
}
