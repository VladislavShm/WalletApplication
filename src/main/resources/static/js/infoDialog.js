class InfoDialog extends React.Component {
    constructor(props) {
        super(props);
        this.state = {activeOperation: null};
    }

    activateOperation = operation => {
        this.setState({
            activeOperation: operation
        });
    };

    cancelOperation = () => {
        this.setState({
            activeOperation: null
        });
    };

    onClose = () => {
        this.setState({
            activeOperation: null
        })
        this.props.onClose();
    }

    render() {
        let state = this.state;
        let wallet = this.props.wallet;

        if (wallet == null) {
            return null;
        }

        return (
            <div>
                <Modal show={true}>
                    <div className="form">
                        <div className="form-header">
                            Wallet
                        </div>

                        <div className="form-content">
                            <div className="wallet-modal-wallet-number">Wallet code: {wallet.code}</div>
                            <div className="wallet-modal-balance">Balance: {formatter.format(wallet.balance)}</div>
                        </div>

                        <div className="form-actions">
                            <button onClick={() => this.activateOperation('TRANSFER')}>
                                Transfer
                            </button>
                            <button onClick={() => this.activateOperation('TOP_UP')}>
                                Top Up
                            </button>
                            <button onClick={() => this.activateOperation('WITHDRAW')}>
                                Withdraw
                            </button>
                            <button onClick={this.onClose}>
                                Close
                            </button>
                        </div>
                    </div>
                </Modal>

                <TransferDialog walletId={wallet.id} show={state.activeOperation === 'TRANSFER'}
                                onClose={this.cancelOperation} maxAmount={wallet.balance}/>

                <TopUpDialog walletId={wallet.id} show={state.activeOperation === 'TOP_UP'}
                             onClose={this.cancelOperation}/>

                <WithdrawDialog walletId={wallet.id} show={state.activeOperation === 'WITHDRAW'}
                                onClose={this.cancelOperation} maxAmount={wallet.balance}/>
            </div>
        )
    }
}
