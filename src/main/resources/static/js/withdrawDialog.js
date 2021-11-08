class WithdrawDialog extends React.Component {
    constructor(props) {
        super(props);
        this.state = {amount: null, formDisabled: false};
    }

    onClose = () => {
        this.setState({amount: null})
        this.props.onClose();
    }

    handleAmount = amountEvent => {
        this.setState({
            amount: amountEvent.target.value
        });
    };

    sendRequest = (event) => {
        event.preventDefault();

        let state = this.state;
        const requestOptions = {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({amount: state.amount, walletId: this.props.walletId})
        };

        this.setState({
            formDisabled: true
        })

        fetch('/api/wallet/withdraw', requestOptions)
            .then(response => {
                if (response.ok) {
                    document.location.reload();
                } else {
                    console.log(response);
                }
            })
            .finally(() => {
                this.setState({
                    formDisabled: false
                })
            });
    }

    render() {
        return (
            <Modal show={this.props.show}>
                <div className="form">
                    <form onSubmit={this.sendRequest.bind(this)}>
                        <div className="form-header">
                            Withdraw operation
                        </div>
                        <div className="form-content">
                            <div className="input-group">
                                <input value={this.state.amount} onChange={this.handleAmount} type="number" min="0.01"
                                       disabled={this.state.formDisabled} step="0.01" required placeholder={"Amount"}
                                       max={this.props.maxAmount}/>
                            </div>
                        </div>
                        <div className="form-actions">
                            <button type="submit" disabled={this.state.formDisabled}>
                                Submit
                            </button>
                            <button onClick={this.onClose} disabled={this.state.formDisabled}>
                                Cancel
                            </button>
                        </div>
                    </form>
                </div>
            </Modal>
        )
    }
}
